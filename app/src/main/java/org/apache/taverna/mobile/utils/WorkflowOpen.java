/*
* Apache Taverna Mobile
* Copyright 2015 The Apache Software Foundation

* This product includes software developed at
* The Apache Software Foundation (http://www.apache.org/).

* Licensed to the Apache Software Foundation (ASF) under one
* or more contributor license agreements. See the NOTICE file
* distributed with this work for additional information
* regarding copyright ownership. The ASF licenses this file
* to you under the Apache License, Version 2.0 (the
* "License"); you may not use this file except in compliance
* with the License. You may obtain a copy of the License at
*
* http://www.apache.org/licenses/LICENSE-2.0
*
* Unless required by applicable law or agreed to in writing,
* software distributed under the License is distributed on an
* "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
* KIND, either express or implied. See the License for the
* specific language governing permissions and limitations
* under the License.
*/

package org.apache.taverna.mobile.utils;

/**
 * Created by Larry Akah on 7/11/15.
 */

import org.apache.taverna.mobile.R;
import org.apache.taverna.mobile.tavernamobile.TavernaPlayerAPI;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Base64;
import android.util.Log;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.Charset;

/**
 * Read the selected xml file from storage and upload to player to generate workflowRun
 */
public class WorkflowOpen extends AsyncTask<String, Void, String> {

    private static final String TAG = "WorkflowOpen";
    TavernaPlayerAPI tavernaPlayerAPI = new TavernaPlayerAPI();
    private Context context;
    private ProgressDialog progressDialog;

    public WorkflowOpen(Context context) {
        this.context = context;
        progressDialog = new ProgressDialog(this.context);
    }

    @Override
    protected void onPreExecute() {

        progressDialog.setMessage("Uploading Workflow ... ");
        progressDialog.show();
    }

    /**
     * @param params path to workflow file to upload to player
     * @return run framework used to create a new workflow run
     */
    @Override
    protected String doInBackground(String... params) {
        StringBuffer sb = new StringBuffer(55);
        String str = "";
        try {

            //prepare connection requests
            File objectFile = new File(params[0]); //the resource xml file representing the
            // workflow to be uploaded to the player
            String playerurl = new TavernaPlayerAPI(this.context).getplayerBaseUrl() + "workflows" +
                    ".json";
            URL posturl = new URL(playerurl);
            HttpURLConnection connection = (HttpURLConnection) posturl.openConnection();

            String user = tavernaPlayerAPI.getPlayerUserName(this.context) + ":" +
                    tavernaPlayerAPI.getPlayerUserPassword(this.context);
            String basicAuth = "Basic " + Base64.encodeToString(user.getBytes(Charset.forName
                    ("UTF-8")), Base64.DEFAULT);
            //read the file from remote resource and encode the stream with a base64 algorithm

            try {
                BufferedReader br = new BufferedReader(new FileReader(objectFile));

                while ((str = br.readLine()) != null) {
                    sb.append(str);
                    sb.append('\n');
                }
                br.close();
            } catch (IOException e) {
                Log.e(TAG, "doInBackground: ", e);
            }

            String data = "{\"document\":\"data:application/octet-stream;base64," +
                    Base64.encodeToString(sb.toString().getBytes(Charset.forName("UTF-8")),
                            Base64.URL_SAFE |
                            Base64.NO_WRAP).replace('-', '+') + "\"}";
            String post = "{\"workflow\":" + data + "}";
            //clear sb so that we can use it again to fetch results from this post request
            sb.delete(0, sb.length() - 1);
            Log.i(TAG, "BODY=>" + post);
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Authorization", basicAuth);
            connection.setRequestProperty("Accept", "*/*");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setRequestProperty("Content-Encoding", "UTF-8");
            connection.setUseCaches(false);
            connection.setDoOutput(true);
            connection.connect(); //send request

            DataOutputStream dos = new DataOutputStream(connection.getOutputStream());

            dos.writeBytes(post); //write post data which is a formatted json data representing
            // body of workflow

            dos.flush();
            dos.close();

            InputStream dis = connection.getInputStream();
            BufferedReader br = new BufferedReader(new InputStreamReader(dis, "UTF-8"));
            while ((str = br.readLine()) != null) {
                sb.append(str);
            }
            Log.i(TAG, "Post Response Code: " + connection.getResponseCode());
            Log.i(TAG, "Post response message: " + connection.getResponseMessage());
            connection.disconnect();
        } catch (IOException e) {
            Log.e(TAG, "doInBackground: ", e);
            sb.append("Error reading remote workflow. Please try again later");
        }

        return sb.toString();
    }

    /**
     * Receives a result from the player as a json describing the workflow that has just been
     * uploaded along with key components that
     * can be used to generate a run from thw workflow. A run is started that would fetch and build
     * a sample UI for a workflow run to be executed
     *
     * @param s the json result that describes the uploaded workflow
     */
    @Override
    protected void onPostExecute(String s) {
        progressDialog.dismiss();
        Log.i(TAG, s);
        s = s.substring(1, s.length());
        try {
            JSONObject workflowJson = new JSONObject(s);
            new WorkflowRunTask(this.context).execute(workflowJson.getString("id"));

        } catch (JSONException e) {
            Log.e(TAG, "onPostExecute: ", e);
        }

    }

    //create and return a new TextView
    public TextView createTextView(Context mcontext, String placeholder) {
        TextView tv = new TextView(mcontext);
        tv.setText(placeholder);
        tv.setMinLines(2);

        return tv;
    }

    //create and return a new EdiText view
    public EditText createEditText(Context ctx, int i) {
        EditText edt;
        edt = new EditText(ctx);
        edt.setHint("Enter Value");
        edt.setMinLines(1);
        edt.setId(i);
        return edt;
    }

    private class WorkflowRunTask extends AsyncTask<String, Void, String> {

        private Context context;
        private AlertDialog.Builder alertDialogBuilder;
        private AlertDialog runDialog;

        private WorkflowRunTask(Context context) {
            this.context = context;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            progressDialog.setMessage(this.context.getResources().getString(R.string.fetchrun));
            progressDialog.show();
        }

        @Override
        protected String doInBackground(String... params) {
            StringBuffer sb = new StringBuffer();
            try {

                URL workflowurl = new URL(new TavernaPlayerAPI(this.context)
                        .mPlayerRunFrameworkUrl + params[0]);
                HttpURLConnection connection = (HttpURLConnection) workflowurl.openConnection();
                String userpass = tavernaPlayerAPI.getPlayerUserName(this.context) + ":" +
                        tavernaPlayerAPI.getPlayerUserPassword(this.context);
                String basicAuth = "Basic " + Base64.encodeToString(userpass.getBytes(Charset
                        .forName("UTF-8")), Base64
                        .DEFAULT);

                connection.setRequestProperty("Authorization", basicAuth);
                connection.setRequestProperty("Accept", "application/json");
                connection.setRequestMethod("GET");
                connection.connect(); //send request
                Log.i("RESPONSE Code", "" + connection.getResponseCode());
                Log.i("RESPONSE Message", "" + connection.getResponseMessage());
                Log.i("Authorization ", "" + connection.getRequestProperty("Authorization"));

                InputStream dis = connection.getInputStream();
                BufferedReader br = new BufferedReader(new InputStreamReader(dis, "UTF-8"));

                String jsonData = "";
                while ((jsonData = br.readLine()) != null) {
                    sb.append(jsonData);
                }
                dis.close();
                br.close();
                return sb.toString();

            } catch (IOException ex) {
                Log.e(TAG, "doInBackground: ", ex);
            }
            return sb.toString();
        }

        @Override
        protected void onPostExecute(String result) {
            //show the skeleton to the user in a dialog box
            final Context ctx = this.context;
            final LinearLayout ll = new LinearLayout(ctx);
            ScrollView sv = new ScrollView(ctx);
            ll.setOrientation(LinearLayout.VERTICAL);
            sv.addView(ll);

            try {
                final JSONObject json = new JSONObject(result); //main server response json
                JSONObject mjson = json.getJSONObject("run"); //main framework response json
                String name = mjson.getString("name"); //a name that can be configured or edited
                // for the new run to be created
                ll.addView(createTextView(ctx, name));
                final JSONArray attr_array = mjson.getJSONArray("inputs_attributes");
                for (int i = 0; i < attr_array.length(); i++) {
                    JSONObject obj = attr_array.getJSONObject(i);
                    ll.addView(createTextView(ctx, obj.getString("name")));
                    ll.addView(createEditText(ctx, i));
                }

                alertDialogBuilder = new AlertDialog.Builder(ctx);
                alertDialogBuilder.setView(sv);
                //               alertDialogBuilder.setMessage(result);
                alertDialogBuilder.setIcon(ctx.getResources().getDrawable(R.mipmap.ic_launcher));
                alertDialogBuilder.setTitle("New Workflow Run");
                alertDialogBuilder.setPositiveButton("Execute", new DialogInterface
                        .OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        int n = attr_array.length();
                        for (int j = 0; j < n; j++) {
                            try {
                                EditText inputText = (EditText) ll.findViewById(j);
                                String value = inputText.getText().toString(); //get input entry
                                // entered by the user
                                JSONObject jojb = attr_array.getJSONObject(j); //get the input
                                // attributes provided by the skeleton
                                jojb.put("value", value); //replace value field in object with
                                // the entry provided by the user
                                attr_array.put(j, jojb); //replace the input entry with the new
                                // name/input json object

                            } catch (JSONException e) {
                                Log.e(TAG, "onClick: ", e);
                            }

                        }
                        try {
                            json.put("inputs_attributes", attr_array);
                            Log.i("RUN FRAMEWORK", json.toString(2));
                            //start a run task to execute the run.
                            new RunTask(ctx).execute(json.toString());
                        } catch (JSONException e) {
                            Log.e(TAG, "onClick: ", e);
                        }

                    }
                });
                alertDialogBuilder.setNegativeButton("Cancel", new DialogInterface
                        .OnClickListener() {

                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        dialogInterface.dismiss();
                    }
                });

                runDialog = alertDialogBuilder.create();

            } catch (JSONException e) {
                Log.e(TAG, "onPostExecute: ", e);
            }
            progressDialog.dismiss();
            runDialog.show();
        }
    }
}



