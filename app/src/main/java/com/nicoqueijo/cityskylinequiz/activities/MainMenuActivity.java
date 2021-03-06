package com.nicoqueijo.cityskylinequiz.activities;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.nicoqueijo.cityskylinequiz.R;
import com.nicoqueijo.cityskylinequiz.helpers.ResourceByNameRetriever;
import com.nicoqueijo.cityskylinequiz.helpers.SystemInfo;
import com.nicoqueijo.cityskylinequiz.models.City;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// JSON file on the cloud:
// https://api.myjson.com/bins/7frmp

/**
 * This is the main activity that runs immediately after the splash screen activity when the app
 * is launched. Serves as the entry point holding pathways to the app's features.
 */
public class MainMenuActivity extends AppCompatActivity {

    public static final String DEVELOPER_GITHUB_URL = "https://github.com/nicoqueijo";
    public static final String DEVELOPER_EMAIL = "queijonicolas@gmail.com";

    public static List<City> cities;
    private String currentLanguage;
    private ActionBar mActionBar;
    private SharedPreferences mSharedPreferences;
    private RelativeLayout mRelativeLayoutPlayGame;
    private RelativeLayout mRelativeLayoutCityList;
    private RelativeLayout mRelativeLayoutSettings;
    private ImageView mImageGameIcon;
    private ImageView mImageCityIcon;
    private ImageView mImageSettingsIcon;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mSharedPreferences = getSharedPreferences("settings", Context.MODE_PRIVATE);
        setTheme(mSharedPreferences.getInt("theme", R.style.AppThemeLight));
        setLocale(mSharedPreferences.getString("language", SystemInfo.SYSTEM_LOCALE));
        setContentView(R.layout.activity_menu_main);
        SharedPreferences.Editor editor = mSharedPreferences.edit();
        currentLanguage = mSharedPreferences.getString("language", SystemInfo.SYSTEM_LOCALE);
        mActionBar = getSupportActionBar();
        mActionBar.setDisplayShowHomeEnabled(true);
        mActionBar.setIcon(R.mipmap.ic_launcher);
        mActionBar.setTitle(R.string.actionbar_app_name);

        showFirstLaunchDialog(editor);
        parseJsonAndCreateCityObjects();

        mRelativeLayoutPlayGame = (RelativeLayout) findViewById(R.id.container_play_game);
        mRelativeLayoutCityList = (RelativeLayout) findViewById(R.id.container_city_list);
        mRelativeLayoutSettings = (RelativeLayout) findViewById(R.id.container_settings);
        mImageGameIcon = (ImageView) findViewById(R.id.icon_play_game);
        mImageCityIcon = (ImageView) findViewById(R.id.icon_city_list);
        mImageSettingsIcon = (ImageView) findViewById(R.id.icon_settings);

        mRelativeLayoutPlayGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentQuizMenu = new Intent(MainMenuActivity.this, QuizMenuActivity.class);
                startActivity(intentQuizMenu);
            }
        });

        mRelativeLayoutCityList.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentCityList = new Intent(MainMenuActivity.this, CityListActivity.class);
                startActivity(intentCityList);
            }
        });

        mRelativeLayoutSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intentSettings = new Intent(MainMenuActivity.this, SettingsActivity.class);
                startActivity(intentSettings);
            }
        });

    }

    /**
     * Loads the theme if it was changed. Updates the city objects with city and country names in
     * its appropriate language if the language was changed.
     */
    @Override
    public void onResume() {
        super.onResume();
        loadTheme();
        updateCitiesWithCurrentLanguage();
    }

    /**
     * Creates a hamburger-style menu with options to view the app source code, make a suggestion,
     * report an error, or rate the app.
     *
     * @param menu The menu to be created.
     * @return Status of the operation.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main_activity, menu);
        return super.onCreateOptionsMenu(menu);
    }

    /**
     * Starts an intent based on the menu item selected. Source code intent opens the developer's
     * Github profile on a browser app. Suggestion intent and report intent open a new email message
     * with a predefined subject. Rate intent opens the app's url in the Google Play store.
     *
     * @param item The menu item being selected.
     * @return Status of the operation.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case (R.id.menu_item_source_code):
                Intent sourceCodeIntent = new Intent(Intent.ACTION_VIEW);
                sourceCodeIntent.setData(Uri.parse(DEVELOPER_GITHUB_URL));
                Intent sourceCodeChooser = Intent.createChooser(sourceCodeIntent, getString(R.string.launch_browser));
                startActivity(sourceCodeChooser);
                break;
            case (R.id.menu_item_suggest):
                Intent emailSuggestionIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"
                        + DEVELOPER_EMAIL));
                emailSuggestionIntent.putExtra("subject", getResources().getString(R.string.app_name)
                        + " - " + getResources().getString(R.string.suggestion));
                Intent emailSuggestionChooser = Intent.createChooser(emailSuggestionIntent,
                        getString(R.string.launch_email));
                startActivity(emailSuggestionChooser);
                break;
            case (R.id.menu_item_report):
                Intent emailIssueIntent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"
                        + DEVELOPER_EMAIL));
                emailIssueIntent.putExtra("subject", getResources().getString(R.string.app_name)
                        + " - " + getResources().getString(R.string.report_error));
                Intent emailIssueChooser = Intent.createChooser(emailIssueIntent,
                        getString(R.string.launch_email));
                startActivity(emailIssueChooser);
                break;
            case (R.id.menu_item_rate):
                Intent rateAppIntent = new Intent(Intent.ACTION_VIEW);
                rateAppIntent.setData(Uri.parse("market://details?id=" + getPackageName()));
                try {
                    startActivity(rateAppIntent);
                } catch (ActivityNotFoundException e) {
                    Toast.makeText(this, R.string.google_play_error, Toast.LENGTH_LONG).show();
                }
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    /**
     * Sets the locale to a new language.
     *
     * @param lang the new language to set the app to.
     */
    public void setLocale(String lang) {
        Locale myLocale = new Locale(lang);
        Resources res = getResources();
        DisplayMetrics dm = res.getDisplayMetrics();
        Configuration conf = res.getConfiguration();
        conf.locale = myLocale;
        res.updateConfiguration(conf, dm);
    }

    /**
     * Gets the resource id of the current theme.
     *
     * @return the resource id as an int.
     */
    private int getThemeId() {
        try {
            Class<?> wrapper = Context.class;
            Method method = wrapper.getMethod("getThemeResId");
            method.setAccessible(true);
            return (Integer) method.invoke(this);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Parses the JSON file stored in the assets folder and returns a String representation of it.
     *
     * @return the JSON file in String format
     */
    public String loadJSONFromAsset() {
        String json = null;
        try {
            InputStream inputStream = getAssets().open("cities.json");
            int size = inputStream.available();
            byte[] buffer = new byte[size];
            inputStream.read(buffer);
            inputStream.close();
            json = new String(buffer, "UTF-8");
        } catch (IOException ex) {
            ex.printStackTrace();
            return null;
        }
        return json;
    }

    /**
     * Takes the String JSON and puts it in a JSON object. Then populates the array of model
     * objects with the data contained in the JSON object.
     */
    private void parseJsonAndCreateCityObjects() {
        cities = new ArrayList<>();
        try {
            JSONObject jsonObject = new JSONObject(loadJSONFromAsset());
            JSONArray jsonArray = jsonObject.getJSONArray("cities");

            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject cityObject = jsonArray.getJSONObject(i);
                cities.add(new City(cityObject.getString("city"), cityObject.getString("country"),
                        cityObject.getString("imageUrl"), cityObject.getString("coordinates"),
                        cityObject.getString("wikiUrl")));
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    /**
     * Checks if either the theme or language has been changed. If either has changed the current
     * activity restarts so the new theme/language can be applied to all views. The main activity
     * icons get switched from light/dark vice-versa depending on the theme change.
     */
    private void loadTheme() {
        if (mSharedPreferences.getInt("theme", R.style.AppThemeLight) != getThemeId()
                || !mSharedPreferences.getString("language", SystemInfo.SYSTEM_LOCALE)
                .equals(currentLanguage)) {
            this.finish();
            final Intent intent = this.getIntent();
            this.startActivity(intent);
        }
        switch (getThemeId()) {
            case (R.style.AppThemeLight):
                mImageGameIcon.setImageResource(R.drawable.ic_dark_game);
                mImageCityIcon.setImageResource(R.drawable.ic_dark_city);
                mImageSettingsIcon.setImageResource(R.drawable.ic_dark_settings);
                break;
            case (R.style.AppThemeDark):
                mImageGameIcon.setImageResource(R.drawable.ic_light_game);
                mImageCityIcon.setImageResource(R.drawable.ic_light_city);
                mImageSettingsIcon.setImageResource(R.drawable.ic_light_settings);
                break;
        }
    }

    /**
     * Updates the city and country names of the model objects upon the language change with the
     * names in that language.
     */
    private void updateCitiesWithCurrentLanguage() {
        for (City city : cities) {
            city.setCityNameInCurrentLanguage(ResourceByNameRetriever
                    .getStringResourceByName(city.getCityName(), this));
            city.setCountryNameInCurrentLanguage(ResourceByNameRetriever
                    .getStringResourceByName(city.getCountryName(), this));
        }
    }

    /**
     * Shows an Alert Dialog to the user if it's their first time launching the app. The
     * dialog is a disclaimer apologizing to the user for possible mistranslated non-english strings.
     *
     * @param editor the SharedPreferences Editor to save and track if it's the first time the
     *               user launches the app.
     */
    private void showFirstLaunchDialog(SharedPreferences.Editor editor) {
        if (mSharedPreferences.getBoolean("first_launch", true)) {
            final AlertDialog.Builder translationDialog = new AlertDialog.Builder(this);
            translationDialog.setTitle(getResources().getString(R.string
                    .language_disclaimer_title));
            translationDialog.setMessage(getResources().getString(R.string
                    .language_disclaimer_message));
            translationDialog.setIcon(R.drawable.ic_translate);
            translationDialog.setCancelable(false);
            translationDialog.setPositiveButton(getResources().getString(R.string.ok),
                    new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    });
            translationDialog.show();
            editor.putBoolean("first_launch", false);
            editor.commit();
        }
    }
}
