package ch.fhnw.locactitrackermobile.service;

/**
 * Created by xavierbutty on 22.12.16.
 */
import ch.fhnw.locactitrackermobile.model.ActivityTrace;
import ch.fhnw.locactitrackermobile.model.TrainingTrace;
import retrofit.client.Response;
import retrofit.http.Body;
import retrofit.http.POST;

public interface RestApi {

    @POST("/recognition")
    public Response sendRecognitionValues(@Body ActivityTrace activityTrace);


    @POST("/training")
    public Response sendTrainingValues(@Body TrainingTrace trainingTrace);


}