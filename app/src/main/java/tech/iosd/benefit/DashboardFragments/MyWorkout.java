package tech.iosd.benefit.DashboardFragments;

import android.app.Presentation;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.support.design.widget.Snackbar;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Adapter;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.thin.downloadmanager.DefaultRetryPolicy;
import com.thin.downloadmanager.DownloadRequest;
import com.thin.downloadmanager.DownloadStatusListener;
import com.thin.downloadmanager.ThinDownloadManager;

import java.io.File;
import java.io.IOException;
import java.io.PipedReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import devs.mulham.horizontalcalendar.HorizontalCalendar;
import devs.mulham.horizontalcalendar.utils.HorizontalCalendarListener;
import retrofit2.adapter.rxjava.HttpException;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;
import rx.subscriptions.CompositeSubscription;
import tech.iosd.benefit.Adapters.DashboardWorkoutAdapter;
import tech.iosd.benefit.Model.*;
import tech.iosd.benefit.Network.NetworkUtil;
import tech.iosd.benefit.R;
import tech.iosd.benefit.VideoPlayer.VideoPlayerActivity;

public class MyWorkout extends Fragment
{
    public Calendar selDate;

    String[] months = {"January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"};
    Context ctx;
    FragmentManager fm;

    private String selectedDate;
    private SimpleDateFormat dateFormat;
    private ProgressDialog progressDialog;
    private CompositeSubscription compositeSubscription, mcompositeSubscription;
    private DatabaseHandler db;
    private RecyclerView recyclerView;
    private DashboardWorkoutAdapter adapter;
    private ArrayList<Exercise>  exercises;
    private Button startWorkout;
    private ThinDownloadManager downloadManager;
    private int currentPosition =0;

    private  AlertDialog.Builder mBuilder;
    private AlertDialog downloadDialog;
    private View mView;
    private ProgressBar progressBar;
    private TextView progressTV;
    private TextView numberOfCurrentVideo;

    private int noOfDiffId =0;
    private int noOfCurrentVideUser=0;
    boolean allVideoDownloaded = false;


    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState)
    {
        View rootView = inflater.inflate(R.layout.dashboard_my_workouts, container, false);
        ctx = rootView.getContext();
        fm = getFragmentManager();
        progressDialog =  new ProgressDialog(getContext());
        progressDialog.setCancelable(false);
        progressDialog.setMessage("working..");

        compositeSubscription = new CompositeSubscription();
        mcompositeSubscription =  new CompositeSubscription();

        downloadManager =  new ThinDownloadManager();

        db = new DatabaseHandler(getContext());

        recyclerView =  rootView.findViewById(R.id.dashboard_my_workouts_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        adapter = new DashboardWorkoutAdapter(exercises,getActivity());

        mBuilder = new AlertDialog.Builder(getActivity());
        mView = getActivity().getLayoutInflater().inflate(R.layout.dialog_download, null);
        progressBar = mView.findViewById(R.id.main_progressbar);
        progressTV =  mView.findViewById(R.id.percentage_tv);
        numberOfCurrentVideo = mView.findViewById(R.id.currentfileDownload);
        mBuilder.setView(mView);
        downloadDialog = mBuilder.create();

        startWorkout = rootView.findViewById(R.id.dashboard_my_workouts_start_workout);
        startWorkout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                downloadDialog.show();
                downloadDialog.setCancelable(false);
                downloadFiles();
            }
        });

        Calendar startDate = Calendar.getInstance();
        startDate.add(Calendar.MONTH, -1);

        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 1);

        HorizontalCalendar horizontalCalendar = new HorizontalCalendar.Builder(rootView, R.id.my_workout_calendar)
                .range(startDate, endDate)
                .datesNumberOnScreen(7)
                .mode(HorizontalCalendar.Mode.DAYS)
                .configure()
                .formatMiddleText("EEE\n").sizeMiddleText(12)
                .formatBottomText("dd").sizeBottomText(26)
                .showTopText(false)
                .end()
                .build();

        final TextView lbl_year = rootView.findViewById(R.id.my_workout_calendar_year);
        final TextView lbl_month = rootView.findViewById(R.id.my_workout_calendar_month);
        dateFormat = new SimpleDateFormat("dd-MM-yyyy");

        selectedDate = dateFormat.format(Calendar.getInstance().getTime());


        horizontalCalendar.setCalendarListener(new HorizontalCalendarListener()
        {
            @Override
            public void onDateSelected(Calendar date, int position)
            {
                selDate = date;
                selectedDate = dateFormat.format(date.getTime());
                lbl_year.setText(String.valueOf(date.get(Calendar.YEAR)));
                lbl_month.setText(months[date.get(Calendar.MONTH)]);
                progressDialog.show();
                getWorkoutData(selectedDate);
            }
        });
        getWorkoutData(selectedDate);
        return rootView;
    }

    private int getNumberOfDifferntId(){
        ArrayList <String> stringForCheck =  new ArrayList<>();
        int value =0;
        for (int i = 0; i<exercises.size();i++){
            String id = exercises.get(i).getExercise().get_id();
            value++;
            for(int j =0;j<stringForCheck.size();j++){
                if(stringForCheck.size() == 0){
                    stringForCheck.add(id);
                }else if(stringForCheck.get(j).equals(id)){
                    break;
                }else if(j== stringForCheck.size()-1){
                    stringForCheck.add(id);
                }
               /* if (j == i){
                    continue;
                }
                if (id.equals(exercises.get(j).getExercise().get_id())){
                    value--;
                    break;
                }*/
                /*if(j == exercises.size()-1){
                    value++;
                }*/
            }
        }

        return stringForCheck.size();
    }
    private void showSnackBarMessage(String message) {

        if (getView() != null) {

            Snackbar.make(getView(),message, Snackbar.LENGTH_LONG).show();

        }
    }
    private void downloadFiles() {
        if (currentPosition>=exercises.size()){
            downloadDialog.hide();
            if(allVideoDownloaded){
                Intent intent = new Intent(getActivity().getApplicationContext(), VideoPlayerActivity.class);
                String dataInString = (new Gson()).toJson(exercises);
                intent.putExtra("dataFromServer",dataInString);
                getContext().startActivity(intent);
            }else {
                showSnackBarMessage("All files not downloaded.\nPlease try again.");
            }
            return;
        }
        File file = new File(getActivity().getFilesDir().toString()+"/videos/"+exercises.get(currentPosition).getExercise().get_id()+".mp4");
        if(file.exists()){
            Toast.makeText(getContext(),"file arleady presenet"+(currentPosition+1),Toast.LENGTH_SHORT).show();
            currentPosition++;
            downloadFiles();
           /* if(currentPosition<exercises.size()){
                getExcercise(exercises.get(currentPosition).getExercise().get_id());
            }*/
        }
        else{
            getExcercise(exercises.get(currentPosition).getExercise().get_id());
        }
    }

    private void getWorkoutData(String date){
        if(!progressDialog.isShowing()){
            progressDialog.show();
        }

        compositeSubscription.add(NetworkUtil.getRetrofit(db.getUserToken()).getWorkoutforDate(date,db.getUserToken())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponseGetMeal,this::handleErrorGetMeal));
    }

    private void handleResponseGetMeal(ResponseForWorkoutForDate responseForWorkoutForDate) {
        progressDialog.hide();
        if (!responseForWorkoutForDate.isSuccess()){
            return;
            //Download completes here
        }
        Log.d("error77"," " +responseForWorkoutForDate.getData().get(0).getWorkout().getExercises().size());
        exercises = responseForWorkoutForDate.getData().get(0).getWorkout().getExercises();
        adapter.setExercises(exercises);
        recyclerView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        recyclerView.getLayoutParams().height = 210*exercises.size();
        noOfDiffId = getNumberOfDifferntId();

    }

    private void handleErrorGetMeal(Throwable error) {
        progressDialog.hide();
        Log.d("error77",error.getMessage());


        if (error instanceof HttpException) {

            Gson gson = new GsonBuilder().create();

            try {

                String errorBody = ((HttpException) error).response().errorBody().string();
//                Response response = gson.fromJson(errorBody,Response.class);
                //showSnackBarMessage(response.getMessage());
               // Log.d("error77",error.getMessage());

                fm.popBackStack();

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
           // Log.d("error77",error.getMessage());

           // showSnackBarMessage("Network Error !");
        }
    }


    private void getExcercise(String url){
        mcompositeSubscription.add(NetworkUtil.getRetrofit(db.getUserToken()).getExerciseVideoUrl(url,db.getUserToken())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .subscribe(this::handleResponseSendMealLog,this::handleError));
    }

    private void handleResponseSendMealLog(ResponseForGetExcerciseVideoUrl reponse) {
        String url = reponse.getData();
        Toast.makeText(getActivity().getApplicationContext(),"url fetch success",Toast.LENGTH_SHORT).show();
        getVideo(url);
    }
    private void handleError(Throwable error) {


        if (error instanceof HttpException) {

            Gson gson = new GsonBuilder().create();
            //showSnackBarMessage("Network Error !");
            Log.d("error77",error.getMessage());

            try {

                String errorBody = ((HttpException) error).response().errorBody().string();
                /*Response response = gson.fromJson(errorBody,Response.class);
                showSnackBarMessage(response.getMessage());*/

            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            Log.d("error77",error.getMessage());

            //showSnackBarMessage("Network Error !");
        }
    }
    public void getVideo(String url) {
        noOfCurrentVideUser++;
        Uri downloadUri = Uri.parse(url);
        Uri destinationUri = Uri.parse(getActivity().getFilesDir().toString()+"/videos/"+exercises.get(currentPosition).getExercise().get_id()+".mp4");
        DownloadRequest downloadRequest = new DownloadRequest(downloadUri)
                .setRetryPolicy(new DefaultRetryPolicy())
                .setDestinationURI(destinationUri).setPriority(DownloadRequest.Priority.HIGH)
                .setDownloadContext(getActivity().getApplicationContext())//Optional
                .setDownloadListener(new DownloadStatusListener() {
                    @Override
                    public void onDownloadComplete(int id) {
                        currentPosition++;
                        Toast.makeText(getActivity().getApplicationContext(),"completed download"+(currentPosition+1),Toast.LENGTH_SHORT).show();
                        allVideoDownloaded = allVideoDownloaded && true;
                        downloadFiles();

                    }

                    @Override
                    public void onDownloadFailed(int id, int errorCode, String errorMessage) {
                        Toast.makeText(getActivity().getApplicationContext(),"failed error in logs TAG error77 ",Toast.LENGTH_SHORT).show();
                        Log.d("error77",errorMessage+"\n"+"of number"+((int)currentPosition+1)+"\nof id: "+exercises.get(currentPosition).getExercise().get_id());
                        currentPosition++;
                        allVideoDownloaded = allVideoDownloaded && true;

                        downloadFiles();

                    }

                    @Override
                    public void onProgress(int id, long totalBytes, long downlaodedBytes, int progress) {
                        double p = (double)downlaodedBytes/totalBytes*100;
                        //Toast.makeText(getActivity().getApplicationContext(),"total"+ totalBytes+"dnld "+downlaodedBytes+"progress "+p,Toast.LENGTH_SHORT).show();
                        progressBar.setProgress((int)p);
                        progressTV.setText(String.format("%.2f", (float)p));
                        numberOfCurrentVideo.setText(String.valueOf(noOfCurrentVideUser)+"/"+noOfDiffId);
                    }
                });
        int downloadId = downloadManager.add(downloadRequest);


    }
}
