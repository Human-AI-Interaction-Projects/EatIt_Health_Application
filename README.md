# "Eat It!" a Food Identifier App
Android application with TensorFlow Lite incorporation to identify foods and track nutrition facts

By Michael Kim and Vivek Jain

# A Holistic Approach to Health and Nutrition
The ease of access to many products have revolutionized the way consumers approach their daily needs. For example, the rise of apps such as GrubHub, Uber Eats, and Postmates have created a networking system that allows for virtually any food to easily arrive at your doorstep. The issue of maintaining a healthy conscious then arises with so much accessibility to restaurants and meals. Few people are aware of what goes into those meals and keeping track of everything can become a hassle. Our application "Eat It!" makes it just as easy to track those meals as it is to order them. Simply give us your dietary preferences or restrictions and hover the camera over your meal. Ingredients, calories, what to eat, and what not to eat all at your fingertips. We make it easy to choose the right option!

# How do we "Eat It!"?
1) Give us your dietary preferences 
2) Take a picture
3) Eat it!

<html><center><img src = "https://scontent-lax3-2.xx.fbcdn.net/v/t1.15752-9/78686190_918895351839104_7791116244547010560_n.png?_nc_cat=107&_nc_ohc=-6kq3RIw_MMAQkWOpEWuZB3GOp9Qg0jZlizUbtNtTIhYRDmb0WlwukcdQ&_nc_ht=scontent-lax3-2.xx&oh=54b7e0e6faf6b4115eecdcd36bc4d971&oe=5E695537"></center></html>


### Features
The features of our application include:
- Calorie tracker
- History on meals eaten
- Option to relabel images with different foods
- Alerts when a meal contains an undesired ingredient.
# Framework 
### Neural Network Architecture
Our neural network was a 

### TensorFlowLite Image Classification
The TensorFlowLite image classification skeleton was utilized within our application. Our model and other features were implemented in place of the features within the skeleton. Additionally, extra classes were created to account for the additional functions of our application. 

### Application
Our application was created with Android Studios. The classes include: 

- MainActivity.java: the main activity that contains the calorie tracker, food history, and maintains your dietary preferences
- PreferencesActivity.java: the opening activity to input your dietary restrictions/preferences; the preferences are stored in a general array
- RetrieveFeedTask.java: AsyncTask to retrieve calories for food labels using the Nutrionix API
``` java
class RetrieveFeedTask extends AsyncTask<String, Void, String> {

    private Exception exception;

    protected String doInBackground(String... urls) {
        if (!isCancelled()) // when the thread has been cancelled
        {
            try {
                URL url = new URL(urls[0]);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();
                Log.d("Mike", url.toString());
                connection.setRequestMethod("GET");    // GRABS THE URL TO OBTAIN INFORMATION FROM THE URL
                Log.d("Mike", "Getmethod");
                connection.setRequestProperty("x-app-id", "c0fab9f9");  // set the headers for required keys and id
                connection.setRequestProperty("x-app-key", "cbde0a2dd183b2900e236b5c7f52b6b8");
                BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                Log.d("Mike", "BufferedReader");
                // get the buffered reader data
                StringBuilder total = new StringBuilder();
                for (String line; (line = in.readLine()) != null; ) {
                    total.append(line).append('\n');
                }
                // parses the string as a JSONElement
                JsonElement jelement = new JsonParser().parse(total.toString());    // PARSES THE JSON LINE AND TURNS IT TO A JSON ELEMENT
                JsonObject jobject = jelement.getAsJsonObject();    // CONVERTS THE JSON ELEMENT INTO A JSON OBJECT
                JsonArray jarray = jobject.get("branded").getAsJsonArray();    // grabs the list object from the json object

                // Grab Json Element and turn to a integer to track calories
                String caloriesString = jarray.get(2).getAsJsonObject().get("nf_calories").toString();
                connection.disconnect();
                return caloriesString;
            } catch (Exception e) {
                this.exception = e;
                e.printStackTrace();
            }
        }
        return null;
    }
}
```

Within the TensorFlowLite Image Classification skeleton, the classifier and camera activity classes were adjusted to account for the features of our project. This includes cycling through the output of our classifier model to ensure dietary restrictions were met and the function of the eat-it button.

# Overview
Overall, the application was able to perform its function of demonstrating a platform for human-computer interaction. The application alerts users of ingredients that do not coincide with their dietary restrictions, adequately classify across the 13 different meals, and allow the user to relabel/correct the incoming classification. An improvement that could be implemented into our design is the utilization of a cloud model. This would allow for much more flexibility in terms of a larger range of meals to classify along with classification at a higher accuracy.

Additional features to implement include:
- Gallery option to save images
- More accurate calorie count for meals
- Option to add multiple servings of a meal
- Model that can be retrained through a user's label correction

# References
The Nutrionix API was utilized within our application to return the calorie value of each meal that the user enters into their log. This allows for flexibility within our model including when the user inputs a label for a meal that was not trained by our model. 

- Nutritionix API: https://www.programmableweb.com/api/nutritionix 

The TensorFlowLite image classification skeleton was utilized within our application. Our model and other features were implemented in place of the features within the skeleton. 

- TensorFlowLite Image Classification: https://github.com/tensorflow/examples

The Keras Food 101 image dataset was utilized for our neural network training set. 

- Keras Food 101 Image Dataset: https://github.com/stratospark/food-101-keras
