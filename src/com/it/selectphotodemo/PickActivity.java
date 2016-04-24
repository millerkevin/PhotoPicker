package com.it.selectphotodemo;


import java.io.FileNotFoundException;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.Toast;

public class PickActivity extends Activity implements OnClickListener {
	private Button bt_capture;
	private Button bt_pick_gallery;
	private ImageView iv;
	private Uri mTempPhotoUri;
	private Uri mCroppedPhotoUri;
	private static final int REQUEST_CODE_CAMERA_WITH_DATA = 1001;
	private static final int REQUEST_CODE_PHOTO_PICKED_WITH_DATA = 1002;
	private static final int REQUEST_CROP_PHOTO = 1003;
	private  int mPhotoPickSize=300;
    
    
    
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.pick_activity);
		mTempPhotoUri = PhotoUtils.generateTempImageUri(this);
		mCroppedPhotoUri = PhotoUtils.generateTempCroppedImageUri(this);
		initWidget();
	}

	public void initWidget() {
		iv = (ImageView) this.findViewById(R.id.pick_iv);
		bt_capture = (Button) this.findViewById(R.id.pick_capture);
		bt_pick_gallery = (Button) this.findViewById(R.id.pick_gallery);
		bt_capture.setOnClickListener(this);
		bt_pick_gallery.setOnClickListener(this);
	}

	@Override
	public void onClick(View v) {
		if (v.getId() == R.id.pick_capture) {
			
			startTakePhotoActivity(mTempPhotoUri);
		} else if (v.getId() == R.id.pick_gallery) {
		
			startPickFromGalleryActivity(mTempPhotoUri);
		}
	}

	
	/**
	 * 拍照
	 * @param photoUri
	 */
	private void startTakePhotoActivity(Uri photoUri) {
		final Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE, null);
		PhotoUtils.addPhotoPickerExtras(intent, photoUri); //outputUri
		startActivityForResult(intent, REQUEST_CODE_CAMERA_WITH_DATA);
	}

	
	/**
	 * 图库
	 * @param photoUri
	 */
	private void startPickFromGalleryActivity(Uri photoUri) {
		final Intent intent = new Intent(Intent.ACTION_GET_CONTENT, null);
		intent.setType("image/*");
		PhotoUtils.addPhotoPickerExtras(intent, photoUri);//outputUri
		startActivityForResult(intent, REQUEST_CODE_PHOTO_PICKED_WITH_DATA);
	}

	
	 /**
     * Sends a newly acquired photo to Gallery for cropping
     */
    public void doCropPhoto(Uri inputUri, Uri outputUri) {
        try {
            // Launch gallery to crop the photo
            final Intent intent = new Intent("com.android.camera.action.CROP");
            intent.setDataAndType(inputUri, "image/*");
            PhotoUtils.addPhotoPickerExtras(intent, outputUri);
            PhotoUtils.addCropExtras(intent, mPhotoPickSize);
            startActivityForResult(intent, REQUEST_CROP_PHOTO);
        } catch (Exception e) {
           Toast.makeText(this, R.string.photoPickerNotFoundText, Toast.LENGTH_LONG).show();
        }
    }
    
    
    
	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (resultCode == Activity.RESULT_OK) {
			switch (requestCode) {
			// Cropped photo was returned
			case REQUEST_CROP_PHOTO: {
				final Uri uri;
				if (data != null && data.getData() != null) {
					uri = data.getData();
				} else {
					uri = mCroppedPhotoUri;
				}
				try {
				// delete the original temporary photo if it exists
				getContentResolver().delete(mTempPhotoUri, null, null);
					Bitmap bitmap=PhotoUtils.getBitmapFromUri(this, uri);
					iv.setImageBitmap(PhotoUtils.getRoundedCornerBitmap(bitmap, bitmap.getWidth()/2));
				} catch (FileNotFoundException e) {
					Toast.makeText(this, R.string.fileNotFoundText, Toast.LENGTH_LONG).show();
				}
				break;
			}

			// Photo was successfully taken or selected from gallery, now crop
			// it.
			case REQUEST_CODE_PHOTO_PICKED_WITH_DATA:
			case REQUEST_CODE_CAMERA_WITH_DATA:
				final Uri uri;
				boolean isWritable = false;
				if (data != null && data.getData() != null) {
					uri = data.getData();
					Log.i("<<", "uri="+data.getData());
				} else {
					uri = mTempPhotoUri;
					isWritable = true;
				}
				final Uri toCrop;
				if (isWritable) {
			      // Since this uri belongs to our file provider, we know that it is writable by us. 
			      //This means that we don't have to save it into another temporary location 
				 //just to be able to crop it.
					toCrop = uri;
				} else {
					toCrop = mTempPhotoUri;
					try {
						PhotoUtils.savePhotoFromUriToUri(this, uri,
								toCrop, false);
					} catch (SecurityException e) {
						Log.d("<<", "Did not have read-access to uri : " + uri);
					}
				}

				doCropPhoto(toCrop, mCroppedPhotoUri);
				break;
			}
		}
	}

}