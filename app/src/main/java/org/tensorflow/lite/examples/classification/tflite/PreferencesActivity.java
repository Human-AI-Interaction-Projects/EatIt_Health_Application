package org.tensorflow.lite.examples.classification.tflite;

import android.os.Bundle;
import org.tensorflow.lite.examples.classification.R;
import android.content.Intent;
import android.app.Activity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;

public class PreferencesActivity extends Activity{

    int[] hold = {0, 0, 0, 0, 0, 0, 0, 0, 0, 0};    // will hold diets

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_preferences);    // sets the activity login screen
        Button pressme = findViewById(R.id.login);
        pressme.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent myIntent = new Intent(PreferencesActivity.this, MainActivity.class);
                myIntent.putExtra("hold", hold); // puts bundle into the intent
                startActivity(myIntent);    // to start the initial screen
            }
        });
    }

    public void onCheckboxClicked(View view) {
        // Is the view now checked?
        boolean checked = ((CheckBox) view).isChecked();

        // Check which checkbox was clicked
        switch(view.getId()) {
            case R.id.vegetarian:
                if (checked) {
                    hold[0] = 1;
                    Log.d("Mike", "Vegetarian");
                } else {
                    hold[0] = 0;
                }
                break;
            case R.id.vegan:
                if (checked) {
                    hold[1] = 1;
                } else {
                    hold[1] = 0;
                }
                break;
            case R.id.nutallergy:
                if (checked) {
                    hold[2] = 1;

                    Log.d("Mike", "Nuts");
                } else {
                    hold[2] = 0;
                }
                break;
            case R.id.glutenfree:
                if (checked) {
                    hold[3] = 1;
                } else {
                    hold[3] = 0;
                }
                break;
            case R.id.sugarfree:
                if (checked) {
                    hold[4] = 1;
                } else {
                    hold[4] = 0;
                }
                break;
            case R.id.noseafood:
                if (checked) {
                    hold[5] = 1;
                } else {
                    hold[5] = 0;
                }
                break;
            case R.id.nopork:
                if (checked) {
                    hold[6] = 1;
                } else {
                    hold[6] = 0;
                }
                break;
            case R.id.nobeef:
                if (checked) {
                    hold[7] = 1;
                } else {
                    hold[7] = 0;

                }
                break;
            case R.id.noegg:
                if (checked) {
                    hold[8] = 1;
                } else {
                    hold[8] = 0;

                }
                break;
            case R.id.lactoseintolerant:
                if (checked) {
                    hold[9] = 1;
                } else {
                    hold[9] = 0;
                }
                break;
            default:
                break;
        }
    }
}