package com.example.qr_codescan;



import java.util.HashMap;
import java.util.Map;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.android.volley.Request.Method;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

public class MainActivity extends Activity {
	private final static int SCANNIN_GREQUEST_CODE = 1;
	/**
	 * 显示扫描结果
	 */
	private TextView mTextView ;
	/**
	 * 显示扫描拍的图片
	 */
	private ImageView mImageView;
	

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mTextView = (TextView) findViewById(R.id.result); 
		mImageView = (ImageView) findViewById(R.id.qrcode_bitmap);
		
		//点击按钮跳转到二维码扫描界面，这里用的是startActivityForResult跳转
		//扫描完了之后调到该界面
		Button mButton = (Button) findViewById(R.id.button1);
		mButton.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				Intent intent = new Intent();
				intent.setClass(MainActivity.this, MipcaActivityCapture.class);
				intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
				startActivityForResult(intent, SCANNIN_GREQUEST_CODE);
			}
		});
	}
	
	

	@Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch (requestCode) {
		case SCANNIN_GREQUEST_CODE:
			if(resultCode == RESULT_OK){
				final Bundle bundle = data.getExtras();
				//显示扫描到的内容
				mTextView.setText(bundle.getString("result"));
				
				RequestQueue mRequestQueue = Volley.newRequestQueue(this);
				
				mRequestQueue.add(new StringRequest(Method.POST,
						"http://wlp.groupsreader.com/clientLogin",
						new Response.Listener<String>() {

							@Override
							public void onResponse(String response) {
								// TODO Auto-generated method stub
								Log.i("volley==============",
										response.toString());
							}
						}, new Response.ErrorListener() {

							@Override
							public void onErrorResponse(VolleyError error) {
								// TODO Auto-generated method stub
								Log.i("volley==============", error.toString());
							}
						}) {

					protected Map<String, String> getParams()
							throws com.android.volley.AuthFailureError {
						HashMap<String, String> param = new HashMap<String, String>();
						param.put("username", "evil");
						param.put("pwd", "evil");
						param.put("uuid", bundle.getString("result"));
						return param;
					};
				});
				
				
				
				
				
				
				
				
				//显示
				mImageView.setImageBitmap((Bitmap) data.getParcelableExtra("bitmap"));
			}
			break;
		}
    }	

}
