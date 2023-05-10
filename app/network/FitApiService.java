package network;

import android.service.autofill.Dataset;

import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.Scope;

import java.util.List;

import javax.sql.DataSource;

import retrofit2.Call;
import retrofit2.http.GET;
import retrofit2.http.Header;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface FitApiService {
    @GET("dataSources")
    Call<List<DataSource>> getDataSources(@Header("Authorization") String accessToken);

    @GET("datasets/{dataSourceId}")
    Call<Dataset> getDataSet(
            @Header("Authorization") String accessToken,
            @Path("dataSourceId") String dataSourceId,
            @Query("startTimeMillis") long startTimeMillis,
            @Query("endTimeMillis") long endTimeMillis);
}

    GoogleSignInOptions signInOptions =
            new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestScopes(new Scope(FitnessScopes.FITNESS_ACTIVITY_READ_WRITE))
                    .requestIdToken(getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build();
    GoogleSignInClient client = GoogleSignIn.getClient(this, signInOptions);
    GoogleSignInAccount account = GoogleSignIn.getLastSignedInAccount(this);
    String accessToken = account.getIdToken();