package com.zhenl.crawler.utils;

import android.content.ContentResolver;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;

/**
 * Created by lin on 19-11-16.
 */
public class FileUtil {

    public static String getFilePathFromContentUri(Uri uri, ContentResolver contentResolver) {
        String filePath = null;
        if (ContentResolver.SCHEME_CONTENT.equals(uri.getScheme())) {
            String[] filePathColumn = {MediaStore.MediaColumns.DATA};
            Cursor cursor = contentResolver.query(uri, filePathColumn, null, null, null);
            if (cursor != null) {
                if (cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
                    filePath = "file://" + cursor.getString(columnIndex);
                }
                cursor.close();
            }
        } else {
            filePath = uri.toString();
        }
        return filePath;
    }
}
