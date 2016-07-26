package com.example.barcodescanner;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;
import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;
import android.content.Intent;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.Toast;
import android.util.Log;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import android.os.AsyncTask;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.net.URL;
import java.net.URLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.net.Uri;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class BarcodeActivity extends Activity implements OnClickListener {
	private Button scanBtn, previewBtn, linkBtn;
	private TextView authorText, titleText, descriptionText, dateText, ratingCountText;
	private LinearLayout starLayout;
	private ImageView thumbView;
	private ImageView[] starViews;
	private Bitmap thumbImg;
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_barcode);
		scanBtn = (Button)findViewById(R.id.scan_button);
		scanBtn.setOnClickListener(this);
		
		previewBtn = (Button)findViewById(R.id.preview_btn);
		previewBtn.setVisibility(View.GONE);
		previewBtn.setOnClickListener(this);
		
		linkBtn = (Button)findViewById(R.id.link_btn);
		linkBtn.setVisibility(View.GONE);
		linkBtn.setOnClickListener(this);
		
		authorText = (TextView)findViewById(R.id.book_author);
		
		titleText = (TextView)findViewById(R.id.book_title);
		
		descriptionText = (TextView)findViewById(R.id.book_description);
		
		dateText = (TextView)findViewById(R.id.book_date);
		
		starLayout = (LinearLayout)findViewById(R.id.star_layout);
		
		ratingCountText = (TextView)findViewById(R.id.book_rating_count);
		
		thumbView = (ImageView)findViewById(R.id.thumb);
		
		starViews=new ImageView[5];
		for(int s=0; s<starViews.length; s++){
		    starViews[s]=new ImageView(this);
		}
		
		if (savedInstanceState != null){
		    authorText.setText(savedInstanceState.getString("author"));
		    titleText.setText(savedInstanceState.getString("title"));
		    descriptionText.setText(savedInstanceState.getString("description"));
		    dateText.setText(savedInstanceState.getString("date"));
		    ratingCountText.setText(savedInstanceState.getString("ratings"));
		    int numStars = savedInstanceState.getInt("stars");//zero if null
		    for(int s=0; s<numStars; s++){
		        starViews[s].setImageResource(R.drawable.star);
		        starLayout.addView(starViews[s]);
		    }
		    starLayout.setTag(numStars);
		    thumbImg = (Bitmap)savedInstanceState.getParcelable("thumbPic");
		    thumbView.setImageBitmap(thumbImg);
		    previewBtn.setTag(savedInstanceState.getString("isbn"));
		    if(savedInstanceState.getBoolean("isEmbed")) previewBtn.setEnabled(true);
		    else previewBtn.setEnabled(false);
		    if(savedInstanceState.getInt("isLink")==View.VISIBLE) linkBtn.setVisibility(View.VISIBLE);
		    else linkBtn.setVisibility(View.GONE);
		    previewBtn.setVisibility(View.VISIBLE);
		}
	}
	public void onClick(View v){
		//respond to clicks
		if(v.getId()==R.id.scan_button){
			//scan
			IntentIntegrator scanIntegrator = new IntentIntegrator(this);
			scanIntegrator.initiateScan();
			}
		else if(v.getId()==R.id.link_btn){
		    //get the url tag
		    String tag = (String)v.getTag();
		    //launch the url
		    Intent webIntent = new Intent(Intent.ACTION_VIEW);
		    webIntent.setData(Uri.parse(tag));
		    startActivity(webIntent);
		}
		else if(v.getId()==R.id.preview_btn){
		    String tag = (String)v.getTag();
		    //launch preview
		    Intent intent = new Intent(this, EmbeddedBook.class);
		    intent.putExtra("isbn", tag);
		    startActivity(intent);
		}

			
		}
	
	public void onActivityResult(int requestCode, int resultCode, Intent intent) {
		//retrieve scan result
		IntentResult scanningResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, intent);
		if (scanningResult != null) {
			//we have a result
			String scanContent = scanningResult.getContents();
			String scanFormat = scanningResult.getFormatName();
			Log.v("SCAN", "content: "+scanContent+" - format: "+scanFormat);
			
			if(scanContent!=null && scanFormat!=null && scanFormat.equalsIgnoreCase("EAN_13")){
				//book search
				previewBtn.setTag(scanContent);
				String bookSearchString = "https://www.googleapis.com/books/v1/volumes?"+
				"q=isbn:"+scanContent+"&key=AIzaSyDU2-rniF18HFSSrmrBL6S1Awu4n2gPVw8";
				new GetBookInfo().execute(bookSearchString);
				}
				else{
				    Toast toast = Toast.makeText(getApplicationContext(),
				        "Not a valid scan!", Toast.LENGTH_SHORT);
				        toast.show();
				}
			
			}
		else{
		    Toast toast = Toast.makeText(getApplicationContext(),
		        "No book scan data received!", Toast.LENGTH_SHORT);
		    toast.show();
		}
		}
	
	private class GetBookInfo extends AsyncTask<String, Void, String> {
		//fetch book info
		
		@Override
		protected String doInBackground(String... bookURLs) {
		//request book info
			StringBuilder bookBuilder = new StringBuilder();
			for (String bookSearchURL : bookURLs) {
				//search urls
				HttpClient bookClient = new DefaultHttpClient();
				try {
				    //get the data
					HttpGet bookGet = new HttpGet(bookSearchURL);
					HttpResponse bookResponse = bookClient.execute(bookGet);
					StatusLine bookSearchStatus = bookResponse.getStatusLine();
					if (bookSearchStatus.getStatusCode()==200) {
					    //we have a result
						HttpEntity bookEntity = bookResponse.getEntity();
						InputStream bookContent = bookEntity.getContent();
						InputStreamReader bookInput = new InputStreamReader(bookContent);
						BufferedReader bookReader = new BufferedReader(bookInput); 
						
						String lineIn;
						while ((lineIn=bookReader.readLine())!=null) {
						    bookBuilder.append(lineIn);
						}
					}
				}
				catch(Exception e){ e.printStackTrace(); }
				}
			return bookBuilder.toString();
		}
		
		protected void onPostExecute(String result) {
			//parse search results
			try{
				//parse results
				previewBtn.setVisibility(View.VISIBLE);
				JSONObject resultObject = new JSONObject(result);
				JSONArray bookArray = resultObject.getJSONArray("items");
				JSONObject bookObject = bookArray.getJSONObject(0);
				JSONObject volumeObject = bookObject.getJSONObject("volumeInfo");
				
				try{ titleText.setText("TITLE: "+volumeObject.getString("title")); }
				catch(JSONException jse){
					titleText.setText("");
					jse.printStackTrace();
				}
				
				StringBuilder authorBuild = new StringBuilder("");
				try{
					JSONArray authorArray = volumeObject.getJSONArray("authors");
					for(int a=0; a<authorArray.length(); a++){
						if(a>0) authorBuild.append(", ");
						authorBuild.append(authorArray.getString(a));
					}
					authorText.setText("AUTHOR(S): "+authorBuild.toString());
				}
				catch(JSONException jse){
					authorText.setText("");
					jse.printStackTrace();
				}
				
				try{ dateText.setText("PUBLISHED: "+volumeObject.getString("publishedDate")); }
				catch(JSONException jse){
					dateText.setText("");
					jse.printStackTrace();
				}
				
				try{ descriptionText.setText("DESCRIPTION: "+volumeObject.getString("description")); }
				catch(JSONException jse){
					descriptionText.setText("");
					jse.printStackTrace();
				}
				try{
					//set stars
					double decNumStars = Double.parseDouble(volumeObject.getString("averageRating"));
					int numStars = (int)decNumStars;
					starLayout.setTag(numStars);
					starLayout.removeAllViews();
					for(int s=0; s<numStars; s++){
					    starViews[s].setImageResource(R.drawable.star);
					    starLayout.addView(starViews[s]);
					}
					}
					catch(JSONException jse){
					    starLayout.removeAllViews();
					    jse.printStackTrace();
					}
				
				try{ ratingCountText.setText(" - "+volumeObject.getString("ratingsCount")+" ratings"); }
				catch(JSONException jse){
				    ratingCountText.setText("");
				    jse.printStackTrace();
				}
				
				try{
				    boolean isEmbeddable = Boolean.parseBoolean
				        (bookObject.getJSONObject("accessInfo").getString("embeddable"));
				    if(isEmbeddable) previewBtn.setEnabled(true);
				    else previewBtn.setEnabled(false);
				}
				catch(JSONException jse){
				    previewBtn.setEnabled(false);
				    jse.printStackTrace();
				}
				
				try{
				    linkBtn.setTag(volumeObject.getString("infoLink"));
				    linkBtn.setVisibility(View.VISIBLE);
				}
				catch(JSONException jse){
				    linkBtn.setVisibility(View.GONE);
				    jse.printStackTrace();
				}
				
				try{
				    JSONObject imageInfo = volumeObject.getJSONObject("imageLinks");
				    new GetBookThumb().execute(imageInfo.getString("smallThumbnail"));
				}
				catch(JSONException jse){
				    thumbView.setImageBitmap(null);
				    jse.printStackTrace();
				}
				}
				
				catch (Exception e) {
				//no result
					e.printStackTrace();
					titleText.setText("NOT FOUND");
					authorText.setText("");
					descriptionText.setText("");
					dateText.setText("");
					starLayout.removeAllViews();
					ratingCountText.setText("");
					thumbView.setImageBitmap(null);
					previewBtn.setVisibility(View.GONE);
				}
			}
		}
	
	private class GetBookThumb extends AsyncTask<String, Void, String> {
		//get thumbnail
		@Override
		protected String doInBackground(String... thumbURLs) {
		//attempt to download image
			try{
				//try to download
				URL thumbURL = new URL(thumbURLs[0]);
				URLConnection thumbConn = thumbURL.openConnection();
				thumbConn.connect();
				
				InputStream thumbIn = thumbConn.getInputStream();
				BufferedInputStream thumbBuff = new BufferedInputStream(thumbIn);
				
				thumbImg = BitmapFactory.decodeStream(thumbBuff);
				
				thumbBuff.close();
				thumbIn.close();
				}
				catch(Exception e) {
				    e.printStackTrace();
				}
			return "";
		}
		
		protected void onPostExecute(String result) {
		    thumbView.setImageBitmap(thumbImg);
		}
		}
	
	protected void onSaveInstanceState(Bundle savedBundle) {
	    savedBundle.putString("title", ""+titleText.getText());
	    savedBundle.putString("author", ""+authorText.getText());
	    savedBundle.putString("description", ""+descriptionText.getText());
	    savedBundle.putString("date", ""+dateText.getText());
	    savedBundle.putString("ratings", ""+ratingCountText.getText());
	    savedBundle.putParcelable("thumbPic", thumbImg);
	    if(starLayout.getTag()!=null)
	        savedBundle.putInt("stars", Integer.parseInt(starLayout.getTag().toString()));
	    savedBundle.putBoolean("isEmbed", previewBtn.isEnabled());
	    savedBundle.putInt("isLink", linkBtn.getVisibility());
	    if(previewBtn.getTag()!=null)
	        savedBundle.putString("isbn", previewBtn.getTag().toString());
	}
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.barcode, menu);
		return true;
	}
	
	

}
