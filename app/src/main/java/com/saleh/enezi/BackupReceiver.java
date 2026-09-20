package com.saleh.enezi;

import android.app.*;
import android.content.*;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class BackupReceiver extends BroadcastReceiver {
    static final int REQ=7199;

    public static void schedule(Context c){
        try{
            AlarmManager am=(AlarmManager)c.getSystemService(Context.ALARM_SERVICE);
            Intent in=new Intent(c,BackupReceiver.class);
            PendingIntent pi=PendingIntent.getBroadcast(c,REQ,in,PendingIntent.FLAG_UPDATE_CURRENT|PendingIntent.FLAG_IMMUTABLE);
            Calendar cal=Calendar.getInstance();
            cal.set(Calendar.HOUR_OF_DAY,23); cal.set(Calendar.MINUTE,59);
            cal.set(Calendar.SECOND,0); cal.set(Calendar.MILLISECOND,0);
            if(cal.getTimeInMillis()<=System.currentTimeMillis()) cal.add(Calendar.DAY_OF_YEAR,1);
            long when=cal.getTimeInMillis();
            if(Build.VERSION.SDK_INT>=23){
                try{ am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi); return; }
                catch(SecurityException ignored){}
            }
            am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP,when,pi);
        }catch(Exception ignored){}
    }

    @Override public void onReceive(Context context,Intent intent){
        if(Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())){ schedule(context); return; }
        backup(context);
        schedule(context);
    }

    static void backup(Context c){
        String fn="نسخة_احتياطية_"+new SimpleDateFormat("yyyy-MM-dd_HH-mm",Locale.US).format(new Date())+".db";
        File src=c.getDatabasePath("enezi.db");
        if(!src.exists()) return;
        File tmp=new File(c.getCacheDir(),"enezi-backup.tmp");
        try{
            SQLiteDatabase d=null;
            try{
                d=SQLiteDatabase.openDatabase(src.getPath(),null,SQLiteDatabase.OPEN_READWRITE);
                try{d.execSQL("PRAGMA wal_checkpoint(FULL)");}catch(Exception ignored){}
            }finally{
                if(d!=null){try{d.close();}catch(Exception ignored){}}
            }
            try(InputStream in=new FileInputStream(src); OutputStream out=new FileOutputStream(tmp)){
                byte[] buf=new byte[16384]; int n;
                while((n=in.read(buf))>0) out.write(buf,0,n);
                out.flush();
            }
            if(!tmp.exists()||tmp.length()==0) throw new IOException("empty backup");

            if(Build.VERSION.SDK_INT>=29){
                ContentValues v=new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME,fn);
                v.put(MediaStore.MediaColumns.MIME_TYPE,"application/octet-stream");
                v.put(MediaStore.MediaColumns.RELATIVE_PATH,"Download/بقالة العزيز خاص");
                Uri u=c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);
                if(u==null) throw new IOException("create backup failed");
                try(InputStream in=new FileInputStream(tmp); OutputStream out=c.getContentResolver().openOutputStream(u)){
                    if(out==null) throw new IOException("open backup failed");
                    byte[] buf=new byte[16384]; int n;
                    while((n=in.read(buf))>0) out.write(buf,0,n);
                }
            }else{
                File dir=new File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_DOWNLOADS),"بقالة العزيز خاص");
                if(!dir.exists() && !dir.mkdirs()) throw new IOException("mkdir failed");
                File dest=new File(dir,fn);
                try(InputStream in=new FileInputStream(tmp); OutputStream out=new FileOutputStream(dest)){
                    byte[] buf=new byte[16384]; int n;
                    while((n=in.read(buf))>0) out.write(buf,0,n);
                }
            }
        }catch(Exception ignored){
        }finally{
            try{tmp.delete();}catch(Exception ignored){}
        }
    }
}
