package org.tensorflow.lite.examples.classification.tflite;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.TextView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.tensorflow.lite.examples.classification.ClassifierActivity;
import org.tensorflow.lite.examples.classification.R;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

import static java.lang.Integer.parseInt;

public class MainActivity extends AppCompatActivity {

    String profile = ""; // used for creating the profile

    String foodhistory = "";    // for maintaining food history

    int caloriesTotal = 0;  // for keeping track of calories for all food eaten

    String currentDay = ""; // keep track of current timestamp

    String caloriesPer = "";    // keep track of calories per food

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Bundle myBundle = getIntent().getExtras();
        int[] hold= myBundle.getIntArray("hold");
        int[] diet = {0, 0, 0, 0, 0, 0, 0, 0};
        // milk, beef, pork, nuts, gluten, seafood, sugar, egg
        // 0,       1,    2,    3,      4,       5,    6,   7

        if (hold[0] == 1)
        {
            profile= profile+"\n\u2022Vegetarian";
            diet[1] = 1;
            diet[2] = 1;
            diet[5] = 1;
            diet[7] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022Vegetarian", "");    // removes the preference from the profile
            diet[1] = 0;
            diet[2] = 0;
            diet[5] = 0;
            diet[7] = 0;
        }
        if (hold[1] == 1)
        {
            profile= profile+"\n\u2022Vegan";
            diet[1] = 1;
            diet[2] = 1;
            diet[5] = 1;
            diet[0] = 1;
            diet[7] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022Vegan", "");    // removes the preference from the profile
            diet[1] = 0;
            diet[2] = 0;
            diet[5] = 0;
            diet[0] = 0;
            diet[7] = 0;
        }
        if (hold[2] == 1)
        {
            profile= profile+"\n\u2022Nut Allergy";
            diet[3] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022Nut Allergy", "");    // removes the preference from the profile
            diet[3] = 0;
        }
        if (hold[3] == 1)
        {
            profile= profile+"\n\u2022Gluten-Free";
            diet[4] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022Gluten-Free", "");    // removes the preference from the profile
            diet[4] = 0;
        }
        if (hold[4] == 1)
        {
            profile= profile+"\n\u2022Sugar-Free";
            diet[6]=1;
        }
        else
        {
            profile = profile.replace("\n\u2022Sugar-Free", "");    // removes the preference from the profile
            diet[6] = 0;
        }
        if (hold[5] == 1)
        {
            profile= profile+"\n\u2022No Seafood";
            diet[5] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022No Seafood", "");    // removes the preference from the profile
            diet[5] = 0;
        }
        if (hold[6] == 1)
        {
            profile= profile+"\n\u2022No Pork";
            diet[2] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022No Pork", "");    // removes the preference from the profile
            diet[2] = 0;
        }
        if (hold[7] == 1)
        {
            profile= profile+"\n\u2022No Beef";
            diet[1] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022No Beef", "");    // removes the preference from the profile
            diet[1] = 0;
        }
        if (hold[8] == 1)
        {
            profile= profile+"\n\u2022No Egg";
            diet[7] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022No Egg", "");    // removes the preference from the profile
            diet[7] = 0;
        }
        if (hold[9] == 1)
        {
            profile= profile+"\n\u2022Lactose Intolerant";
            diet[0] = 1;
        }
        else
        {
            profile = profile.replace("\n\u2022Lactose Intolerant", "");    // removes the preference from the profile
            diet[0] = 0;
        }
        if (profile == "")
        {
            profile = "\nNo restrictions";
        }
        else
        {
            profile = profile.replace("\nNo restrictions", "");    // removes the preference from the profile
        }

        TextView layoutText = findViewById(R.id.profile);  // grabs the textview from the main activity layout
        layoutText.setText(profile);   // sets the text of the user to match their preferences

        FloatingActionButton fab = findViewById(R.id.camera);   // button for the camera
        fab.setOnClickListener((View v) -> {    // when the camera fab is pressed
            // Move to the live camera activity
            Intent camera = new Intent(MainActivity.this, ClassifierActivity.class);    // creates intent to move to live camera feed
            camera.putExtra("diet", diet);  // sends the preferences over
            startActivityForResult(camera, 1);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 1) {
            if (resultCode == Activity.RESULT_OK) {
                String meal = data.getStringExtra("food");  // gets the string from the classifier activity
                Log.d("Mike", meal);

                // Now lets get the calories for the dish and add it to what we have already
                String calories = nutrientsAPI(meal);

                // in try block because API not recognize
                int check = 0;
                if (calories != null){
                    check =  Integer.parseInt(calories);   // adds into calories count
                }
                // grabs textviews from layout
                caloriesTotal = caloriesTotal + check;

                TextView total = findViewById(R.id.caloriescount);
                TextView percent = findViewById(R.id.caloriespercent);
                TextView foodList = findViewById(R.id.foodhistory);
                TextView caloriesPerText = findViewById(R.id.caloriesperfood);

                // sets the gathered data into the textview for calories

                total.setText(String.valueOf(caloriesTotal));
                percent.setText(String.valueOf((float)caloriesTotal/20) + "%");

                // Grab current day
                Date c = Calendar.getInstance().getTime();
                System.out.println("Current time => " + c);

                SimpleDateFormat df = new SimpleDateFormat("dd-MMM-yyyy");
                String formattedDate = df.format(c);

                if (!currentDay.equals(formattedDate))    // case that the currentDay is not equal to the new timestamp
                {
                    Log.d("Mike", currentDay);
                    Log.d("Mike", formattedDate);
                    currentDay = formattedDate; // sets the new timestamp
                    foodhistory = foodhistory+ "\n" + currentDay +"\n\u2022" + meal;    // adds the current day
                    foodList.setText(foodhistory);
                    caloriesPer = caloriesPer + "\n\n" + calories;
                    caloriesPerText.setText(caloriesPer);

                }
                else    // the timestamp is the same
                {
                    foodhistory = foodhistory + "\n\u2022" + meal;  // adds into the food history
                    foodList.setText(foodhistory);
                    caloriesPer = caloriesPer + "\n" + calories;
                    caloriesPerText.setText(caloriesPer);
                }
            }
        }
    }

    public String nutrientsAPI(String food){
        // API URL: https://trackapi.nutritionix.com/v2/search/instant?query=
        String query = food.replace(" ", "%");  // replaces the whitespace with url character

        // Replace whitespace in food with
        try {
            String foodurl = "https://trackapi.nutritionix.com/v2/search/instant?query=";
            foodurl = foodurl+query;    // combines the request and the url
            foodurl = foodurl.replaceAll("%", "%25");
            URL url = new URL(foodurl);	// CREATES A NEW URL OF THE COMBINED API URL

            AsyncTask mtask = new RetrieveFeedTask().execute(foodurl);
            String output = mtask.get().toString();
            mtask.cancel(true);
            return output;
        } catch (Exception e) {
            return null;
        }
    }
}
