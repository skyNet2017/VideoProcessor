package com.hss01248.videocompress.compare;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class VideoCopyer {

    private static final String TAG = "MediaStoreHelper";

    public static boolean copyVideoToXCompressed(Context context, File file, String fileName) {
        ContentResolver resolver = context.getContentResolver();

        Uri videoCollection;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            videoCollection = MediaStore.Video.Media.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        } else {
            videoCollection = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
        }

        ContentValues newVideoDetails = new ContentValues();
        newVideoDetails.put(MediaStore.Video.Media.DISPLAY_NAME, fileName);
        newVideoDetails.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");

        // specifying subdirectory for the file
        newVideoDetails.put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/XCompressed");

        Uri newVideoUri = null;
        try (InputStream inputStream = new FileInputStream(file)) {
            if (inputStream == null) {
                Log.e(TAG, "Failed to open source URI");
                return false;
            }

            newVideoUri = resolver.insert(videoCollection, newVideoDetails);
            if (newVideoUri == null) {
                Log.e(TAG, "Failed to create new MediaStore entry");
                return false;
            }

            try (OutputStream outputStream = resolver.openOutputStream(newVideoUri)) {
                if (outputStream == null) {
                    Log.e(TAG, "Failed to get output stream");
                    return false;
                }

                byte[] buffer = new byte[4096];
                int bytesRead;

                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                inputStream.close();
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "IO Exception while copying video", e);
            if (newVideoUri != null) {
                // Clean up by deleting the partially copied file
                resolver.delete(newVideoUri, null, null);
            }
            return false;
        } catch (SecurityException e) {
            Log.e(TAG, "Security exception, check permissions", e);
            return false;
        }
    }
}

