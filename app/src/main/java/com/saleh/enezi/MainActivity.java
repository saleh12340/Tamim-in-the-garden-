package com.saleh.enezi;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.Rect;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.provider.MediaStore;
import android.text.Layout;
import android.text.TextPaint;
import android.text.StaticLayout;
import android.text.TextUtils;
import android.text.InputType;
import android.text.method.DigitsKeyListener;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.net.Uri;
import androidx.core.content.FileProvider;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.InputStream;
import java.util.Set;
import java.util.UUID;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Drawable;
import android.content.*;
import android.content.pm.PackageManager;
import android.provider.ContactsContract;
import android.database.Cursor;
import android.database.sqlite.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class MainActivity extends Activity {
    static final int REQ_CONTACTS=4101, PICK_CONTACT=4102, REQ_CAMERA_SCAN=4103, REQ_GALLERY_SCAN=4104, REQ_PERM_CAMERA=4105;
    EditText customerNameInput, customerPhoneInput;
    static final int GREEN=Color.rgb(24,112,61), DARK=Color.rgb(20,70,40), GOLD=Color.rgb(232,169,45), BLUE=Color.rgb(35,105,205), RED=Color.rgb(190,55,45);
    static final int BG=Color.rgb(246,248,246), TEXT=Color.rgb(32,43,36), MUTED=Color.rgb(105,116,108), CARD=Color.WHITE;
    DB db; LinearLayout root,content,bottom; TextView pageTitle; int textSize=16; String currentPage="الرئيسية"; ArrayDeque<String> pageStack=new ArrayDeque<>(); long currentNotePageId=-1; int noteFontSize=14; boolean noteScrollMode=true;
    Uri cameraScanTempUri; Bitmap scanRawBitmap; String scanFilterMode="magic"; float scanRotation=0; String scanCategoryFilter="الكل"; String scanSearchQuery="";

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        getWindow().setStatusBarColor(DARK);
        getWindow().setNavigationBarColor(DARK);
        db=new DB(this); BackupReceiver.schedule(this); home();
    }

    void confirmExit(){
        new AlertDialog.Builder(this)
            .setTitle("تأكيد الخروج")
            .setMessage("هل تريد الخروج من التطبيق؟")
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("خروج",(d,w)->finish())
            .show();
    }
    @Override public void onBackPressed(){ goBack(); }
    void goBack(){
        if(pageStack.isEmpty()){ confirmExit(); return; }
        String prev=pageStack.pop();
        if(prev.equals("الرئيسية")) home();
        else if(prev.equals("الحسابات")||prev.equals("العملاء")) customers();
        else if(prev.equals("الفواتير")) invoiceHistory();
        else if(prev.equals("فواتير الشراء")) purchaseInvoices();
        else if(prev.equals("المخزون")) inventory();
        else if(prev.equals("التقارير")) reports(); else if(prev.equals("الملاحظات")) notes();
        else if(prev.equals("ماسح الفواتير")||prev.equals("الماسح الضوئي")) scanner();
        else home();
    }

    GradientDrawable rounded(int color,float radius){ GradientDrawable g=new GradientDrawable(); g.setColor(color); g.setCornerRadius(radius); return g; }
    GradientDrawable outlined(int color,int stroke,float radius){ GradientDrawable g=rounded(color,radius); g.setStroke(stroke,Color.rgb(224,230,225)); return g; }
    float fitText(float z){return Math.max(9f,Math.min(z,16f));}
    void fitInside(View v,float maxSp,float minSp){
        if(v instanceof TextView){
            TextView t=(TextView)v;
            t.setIncludeFontPadding(true);
            t.setHorizontallyScrolling(false);
            t.setEllipsize(null);
            t.setBreakStrategy(android.text.Layout.BREAK_STRATEGY_HIGH_QUALITY);
            if(android.os.Build.VERSION.SDK_INT>=26){
                t.setAutoSizeTextTypeUniformWithConfiguration(Math.round(minSp),Math.round(maxSp),1,android.util.TypedValue.COMPLEX_UNIT_SP);
            }
        }
    }
    TextView tv(String s,float z){
        TextView v=new TextView(this); v.setText(s); v.setTextSize(fitText(z)); v.setTextColor(TEXT);
        v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); v.setPadding(dp(6),dp(2),dp(6),dp(2));
        v.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); v.setTextDirection(View.TEXT_DIRECTION_RTL);
        fitInside(v,fitText(z),8f); return v;
    }
    Button button(String s){
        Button b=new Button(this); b.setText(s); b.setTextSize(fitText(11)); b.setAllCaps(false); b.setMinHeight(0);
        b.setMinimumHeight(0); b.setPadding(dp(5),dp(0),dp(5),dp(0)); b.setGravity(Gravity.CENTER); b.setStateListAnimator(null);
        b.setIncludeFontPadding(true); b.setMaxLines(3); b.setEllipsize(null);
        b.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); fitInside(b,12f,8f); return b;
    }
    EditText field(String h){
        EditText e=new EditText(this); e.setHint(h); e.setTextSize(13); e.setSingleLine(true); e.setIncludeFontPadding(true); e.setMaxLines(1); fitInside(e,14f,9f);
        e.setTextColor(TEXT); e.setHintTextColor(MUTED); e.setPadding(dp(7),dp(2),dp(7),dp(2)); e.setBackground(outlined(CARD,1,10)); e.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); e.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); e.setTextDirection(View.TEXT_DIRECTION_RTL);
        e.setSelectAllOnFocus(true); e.setOnClickListener(v -> e.selectAll());
        e.setOnFocusChangeListener((v,has)->{ if(has) e.postDelayed(() -> { e.selectAll(); },60); });
        return e;
    }
    EditText numberField(String h){
        EditText e=field(h);
        e.setInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setRawInputType(InputType.TYPE_CLASS_NUMBER|InputType.TYPE_NUMBER_FLAG_DECIMAL);
        e.setKeyListener(DigitsKeyListener.getInstance("0123456789."));
        return e;
    }
    EditText phoneField(String h){
        EditText e=field(h);
        e.setInputType(InputType.TYPE_CLASS_PHONE);
        e.setRawInputType(InputType.TYPE_CLASS_PHONE);
        return e;
    }
    void addField(EditText e){content.addView(e,new LinearLayout.LayoutParams(-1,dp(38))); addSpace(2);}
    void addSpace(int h){Space s=new Space(this); content.addView(s,new LinearLayout.LayoutParams(1,dp(h)));}
    TextView section(String s){TextView v=tv(s,11);v.setTextColor(GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);v.setSingleLine(true);v.setMaxLines(1);v.setEllipsize(null);v.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);v.setPadding(dp(5),0,dp(5),0);v.setBackground(outlined(CARD,1,8));fitInside(v,12f,8f);content.addView(v,new LinearLayout.LayoutParams(-1,dp(25)));addSpace(3);return v;}

    void base(String title){
        if(!title.equals(currentPage)){
            pageStack.push(currentPage);
            currentPage=title;
        }
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG); root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL); bar.setPadding(dp(6),dp(3),dp(6),dp(3)); bar.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,DARK}));
        Button back=button("‹");
        back.setTextColor(Color.WHITE); back.setTextSize(28); back.setBackgroundColor(Color.TRANSPARENT);
        back.setContentDescription("رجوع للشاشة السابقة"); back.setOnClickListener(v->goBack());
        bar.addView(back,new LinearLayout.LayoutParams(dp(46),dp(40)));
        TextView logo=tv("بقالة العزي",19); logo.setTextColor(Color.WHITE); logo.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        bar.addView(logo,new LinearLayout.LayoutParams(0,dp(40),1));
        TextView pt=tv(title,14); pt.setTextColor(Color.WHITE); pt.setGravity(Gravity.CENTER);
        bar.addView(pt,new LinearLayout.LayoutParams(dp(105),dp(38))); root.addView(bar);
        ScrollView sv=new ScrollView(this); sv.setFillViewport(true); sv.setClipToPadding(false);
        content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL); content.setPadding(dp(5),dp(4),dp(5),dp(8)); content.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        if(!"الرئيسية".equals(title)){ TextView operationChip=tv("العملية الحالية: "+title,10); operationChip.setTextColor(GREEN); operationChip.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); operationChip.setSingleLine(true); operationChip.setMaxLines(1); operationChip.setEllipsize(TextUtils.TruncateAt.END); operationChip.setPadding(dp(8),0,dp(8),0); operationChip.setBackground(outline(Color.rgb(241,247,242),8)); content.addView(operationChip,new LinearLayout.LayoutParams(-1,dp(24))); addSpace(2); }
        sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
        setContentView(root);
    }
    void navigate(String n){hideKeyboard(); if(n.equals("الرئيسية"))home();else if(n.equals("العملاء")||n.equals("الحسابات"))customers();else if(n.equals("الفواتير"))invoice();else if(n.equals("فواتير الشراء"))purchaseInvoices();else if(n.equals("المخزون"))inventory();else if(n.equals("ماسح الفواتير")||n.equals("الماسح الضوئي"))scanner();else reports();}
    void importContact(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission("android.permission.READ_CONTACTS")!=PackageManager.PERMISSION_GRANTED){ requestPermissions(new String[]{"android.permission.READ_CONTACTS"},REQ_CONTACTS); return; }
        try{ Intent i=new Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI); startActivityForResult(i,PICK_CONTACT); }catch(Exception e){ Toast.makeText(this,"تعذر فتح جهات الاتصال",Toast.LENGTH_SHORT).show(); }
    }
    @Override protected void onActivityResult(int requestCode,int resultCode,Intent data){
        super.onActivityResult(requestCode,resultCode,data);
        if(requestCode==PICK_CONTACT&&resultCode==RESULT_OK&&data!=null){
            Cursor c=null;
            try{
                c=getContentResolver().query(data.getData(),new String[]{ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,ContactsContract.CommonDataKinds.Phone.NUMBER},null,null,null);
                if(c!=null&&c.moveToFirst()){
                    String n=c.getString(0),p=c.getString(1);
                    if(customerNameInput!=null)customerNameInput.setText(n==null?"":n);
                    if(customerPhoneInput!=null)customerPhoneInput.setText(p==null?"":p);
                    if(customerNameInput!=null)customerNameInput.requestFocus();
                    Toast.makeText(this,"تم استيراد اسم العميل ورقم الهاتف",Toast.LENGTH_SHORT).show();
                }
            }catch(Exception e){Toast.makeText(this,"تعذر قراءة بيانات جهة الاتصال",Toast.LENGTH_SHORT).show();}
            finally{if(c!=null)c.close();}
        }else if(requestCode==8801&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            try{
                if(db!=null)db.close();
                restoreDatabaseFromUri(this,data.getData());
                db=new DB(this);
                Toast.makeText(this,"تم استرجاع النسخة الاحتياطية بنجاح.",Toast.LENGTH_LONG).show();
                home();
            }catch(Exception e){Toast.makeText(this,"تعذر استرجاع النسخة الاحتياطية: "+e.getMessage(),Toast.LENGTH_LONG).show();}
        }else if(requestCode==REQ_CAMERA_SCAN&&resultCode==RESULT_OK){
            handleScanCameraResult(data);
        }else if(requestCode==REQ_GALLERY_SCAN&&resultCode==RESULT_OK&&data!=null&&data.getData()!=null){
            handleScanGalleryResult(data.getData());
        }
    }

    void hideKeyboard(){View v=getCurrentFocus();if(v!=null){((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(v.getWindowToken(),0);v.clearFocus();}}

    TextView cardTitle(String title,String sub){
        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(14,8,14,8);c.setBackground(outlined(CARD,1,16));c.setElevation(2);
        TextView a=tv(title,17);a.setTextColor(GREEN);a.setTypeface(Typeface.DEFAULT,Typeface.BOLD);c.addView(a);
        TextView b=tv(sub,12);b.setTextColor(MUTED);c.addView(b);LinearLayout.LayoutParams cp=new LinearLayout.LayoutParams(-1,dp(72)); cp.setMargins(0,0,0,7); content.addView(c,cp);return a;
    }    void addAction(String a,String sub,View.OnClickListener l){        LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(12,5,12,5);c.setBackground(outlined(CARD,1,16));c.setElevation(2);
        Button b=button(a);b.setTextSize(13);b.setTextColor(TEXT);b.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);b.setOnClickListener(l);c.addView(b,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView s=tv(sub,12);s.setTextColor(MUTED);c.addView(s,new LinearLayout.LayoutParams(-1,30));LinearLayout.LayoutParams ap=new LinearLayout.LayoutParams(-1,dp(82)); ap.setMargins(0,0,0,7); content.addView(c,ap);
    }

    void home(){
        currentPage="الرئيسية";
        pageStack.clear();

        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(BG);
        root.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // الرأس: ثابت ولا يتحرك مع محتوى التطبيق.
        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        header.setPadding(dp(10),dp(5),dp(10),dp(5));
        header.setBackground(new GradientDrawable(GradientDrawable.Orientation.TL_BR,new int[]{GREEN,DARK}));

        LinearLayout titleBox=new LinearLayout(this);
        titleBox.setOrientation(LinearLayout.VERTICAL);
        titleBox.setGravity(Gravity.CENTER);
        TextView title=tv("بقالة العزي للمواد الغذائية",19);
        title.setTextColor(Color.WHITE); title.setTypeface(Typeface.DEFAULT,Typeface.BOLD); title.setGravity(Gravity.CENTER);
        TextView phone=tv("776425052  •  نظام الفواتير والحسابات",10);
        phone.setTextColor(Color.WHITE); phone.setGravity(Gravity.CENTER);
        titleBox.addView(title,new LinearLayout.LayoutParams(-1,dp(28)));
        titleBox.addView(phone,new LinearLayout.LayoutParams(-1,dp(22)));
        header.addView(titleBox,new LinearLayout.LayoutParams(0,dp(54),1));
        root.addView(header,new LinearLayout.LayoutParams(-1,dp(64)));

        // الوسط: تبويبات التطبيق + شاشة القسم.
        ScrollView middleScroll=new ScrollView(this);
        middleScroll.setFillViewport(true);
        middleScroll.setClipToPadding(false);
        LinearLayout middle=new LinearLayout(this);
        middle.setOrientation(LinearLayout.VERTICAL);
        middle.setPadding(dp(6),dp(7),dp(6),dp(8));
        middle.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView tabTitle=tv("تبويبات التطبيق",11);
        tabTitle.setTextColor(GREEN); tabTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        tabTitle.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        middle.addView(tabTitle,new LinearLayout.LayoutParams(-1,dp(25)));

        LinearLayout tabs1=new LinearLayout(this);
        tabs1.setOrientation(LinearLayout.HORIZONTAL); tabs1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] t1={"👥 العملاء والحسابات","🧾 فواتير البيع"};
        View.OnClickListener[] a1={v->customers(),v->invoiceHistory()};
        for(int i=0;i<2;i++){
            Button b=action(t1[i],GREEN); b.setTextSize(13); b.setMaxLines(1); fitInside(b,14f,10f);
            b.setOnClickListener(a1[i]);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(52),1); p.setMargins(i==0?0:dp(3),0,i==0?dp(3):0,0);
            tabs1.addView(b,p);
        }
        middle.addView(tabs1,new LinearLayout.LayoutParams(-1,dp(54))); addSpaceTo(middle,4);

        LinearLayout tabs2=new LinearLayout(this);
        tabs2.setOrientation(LinearLayout.HORIZONTAL); tabs2.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] t2={"🛒 فواتير الشراء","📊 التقارير"};
        View.OnClickListener[] a2={v->purchaseInvoices(),v->reports()};
        for(int i=0;i<2;i++){
            Button b=action(t2[i],i==0?GOLD:BLUE); b.setTextSize(11); b.setMaxLines(1); fitInside(b,12f,9f);
            b.setOnClickListener(a2[i]);
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(44),1); p.setMargins(i==0?0:dp(3),0,i==0?dp(3):0,0);
            tabs2.addView(b,p);
        }
        middle.addView(tabs2,new LinearLayout.LayoutParams(-1,dp(56))); addSpaceTo(middle,5);

        // وصول سريع للماسح والمخزون والملاحظات والإجراءات العامة.
        LinearLayout tabs3=new LinearLayout(this);
        tabs3.setOrientation(LinearLayout.HORIZONTAL); tabs3.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button scannerTab=action("📷 ماسح الفواتير ⚡",Color.rgb(18,140,75)); scannerTab.setTextSize(12); scannerTab.setMaxLines(1); fitInside(scannerTab,13f,9f);
        scannerTab.setOnClickListener(v->scanner());
        Button inventoryTab=action("📦 المخزون",GREEN); inventoryTab.setTextSize(12); inventoryTab.setMaxLines(1); fitInside(inventoryTab,13f,9f);
        inventoryTab.setOnClickListener(v->inventory());
        tabs3.addView(scannerTab,new LinearLayout.LayoutParams(0,dp(50),1));
        LinearLayout.LayoutParams ip=new LinearLayout.LayoutParams(0,dp(50),1); ip.setMargins(dp(3),0,0,0); tabs3.addView(inventoryTab,ip);
        middle.addView(tabs3,new LinearLayout.LayoutParams(-1,dp(52))); addSpaceTo(middle,5);

        LinearLayout tabs4=new LinearLayout(this);
        tabs4.setOrientation(LinearLayout.HORIZONTAL); tabs4.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button notesTab=action("📝 الملاحظات",Color.rgb(125,70,170)); notesTab.setTextSize(11); notesTab.setMaxLines(1); fitInside(notesTab,13f,9f); notesTab.setOnClickListener(v->notes());
        Button generalTab=action("⚡ إجراء عام",BLUE); generalTab.setTextSize(12); generalTab.setMaxLines(1); fitInside(generalTab,13f,9f);
        generalTab.setOnClickListener(v->showGeneralActions());
        tabs4.addView(notesTab,new LinearLayout.LayoutParams(0,dp(48),1));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(0,dp(48),1); gp.setMargins(dp(3),0,0,0); tabs4.addView(generalTab,gp);
        middle.addView(tabs4,new LinearLayout.LayoutParams(-1,dp(50))); addSpaceTo(middle,6);

        LinearLayout screen=card();
        screen.setPadding(dp(9),dp(7),dp(9),dp(7));
        TextView st=tv("شاشة التطبيق",13); st.setTextColor(GREEN); st.setTypeface(Typeface.DEFAULT,Typeface.BOLD); st.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        screen.addView(st,new LinearLayout.LayoutParams(-1,dp(28)));
        TextView desc=tv("اختر أحد التبويبات أعلاه للوصول إلى العملاء، فواتير البيع، فواتير الشراء، ماسح الفواتير الذكي (CamScanner)، المخزون والتقارير.",11);
        desc.setTextColor(MUTED); desc.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); desc.setMaxLines(3); fitInside(desc,11f,8f);
        screen.addView(desc,new LinearLayout.LayoutParams(-1,dp(52)));
        middle.addView(screen,new LinearLayout.LayoutParams(-1,dp(92))); addSpaceTo(middle,6);

        LinearLayout stats=card(); stats.setPadding(dp(7),dp(4),dp(7),dp(4));
        TextView sv=tv("الفواتير "+db.invoiceCount()+"  •  المبيعات "+fmt(db.sales())+" ريال  •  العملاء "+db.customerCount()+"  •  الماسح "+db.scannedInvoiceCount(),11);
        sv.setGravity(Gravity.CENTER); sv.setTextColor(TEXT);
        stats.addView(sv,new LinearLayout.LayoutParams(-1,dp(34)));
        middle.addView(stats,new LinearLayout.LayoutParams(-1,dp(44)));

        middleScroll.addView(middle);
        root.addView(middleScroll,new LinearLayout.LayoutParams(-1,0,1));

        // الأسفل: النسخ الاحتياطي والزر السريع ثابتان خارج منطقة التمرير.
        LinearLayout footer=new LinearLayout(this);
        footer.setOrientation(LinearLayout.HORIZONTAL);
        footer.setGravity(Gravity.CENTER_VERTICAL);
        footer.setPadding(dp(6),dp(4),dp(6),dp(4));
        footer.setBackground(outlined(CARD,1,0));

        Button backup=button("💾 النسخ الاحتياطي");
        backup.setTextColor(GREEN); backup.setTextSize(10); backup.setMaxLines(2); fitInside(backup,11f,8f);
        backup.setOnClickListener(v->showBackupRestore());
        footer.addView(backup,new LinearLayout.LayoutParams(0,dp(48),1));

        Button quick=action("⚡ فاتورة جديدة",GOLD);
        quick.setTextSize(12); quick.setMaxLines(1); fitInside(quick,13f,9f);
        quick.setOnClickListener(v->invoice());
        footer.addView(quick,new LinearLayout.LayoutParams(0,dp(48),1.35f));
        root.addView(footer,new LinearLayout.LayoutParams(-1,dp(56)));

        setContentView(root);
    }

    void showGeneralActions(){
        String[] choices={"🧾 فاتورة مبيعات جديدة","🛒 فاتورة شراء جديدة","📷 ماسح الفواتير (CamScanner)","👥 إضافة عميل","📦 إضافة صنف","📊 التقارير"};
        new AlertDialog.Builder(this).setTitle("إجراء عام").setItems(choices,(d,w)->{
            if(w==0) invoice();
            else if(w==1) newPurchaseInvoice();
            else if(w==2) scanner();
            else if(w==3) customers();
            else if(w==4) inventory();
            else reports();
        }).setNegativeButton("إغلاق",null).show();
    }

    void invoice(){invoice(false,-1);}
    void invoice(boolean edit,long invoiceId){
        base(edit?"تعديل الفاتورة":"فاتورة جديدة");
        section("بيانات الفاتورة");

        // صف واحد مضغوط: رقم الفاتورة + اسم العميل + التاريخ والوقت.
        LinearLayout metaRow=new LinearLayout(this);
        metaRow.setOrientation(LinearLayout.HORIZONTAL);
        metaRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        TextView no=tv(edit?db.invoiceNo(invoiceId):String.valueOf(db.nextInvoice()),15);
        no.setTextColor(GREEN);no.setTypeface(Typeface.DEFAULT,Typeface.BOLD);no.setGravity(Gravity.CENTER);
        no.setBackground(outline(Color.rgb(248,250,248),14));
        no.setContentDescription("رقم الفاتورة");
        metaRow.addView(no,new LinearLayout.LayoutParams(0,dp(46),0.72f));

        AutoCompleteTextView customer=new AutoCompleteTextView(this);
        customer.setHint("اسم العميل");customer.setTextSize(14);customer.setSingleLine(true);
        customer.setTextColor(TEXT);customer.setHintTextColor(MUTED);
        customer.setPadding(dp(10),dp(6),dp(10),dp(6));customer.setBackground(outline(CARD,14));
        customer.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        customer.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);customer.setTextDirection(View.TEXT_DIRECTION_RTL);
        customer.setThreshold(1);customer.setSelectAllOnFocus(true);
        customer.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,db.customerNames()));
        metaRow.addView(customer,new LinearLayout.LayoutParams(0,dp(46),1.35f));

        TextView dt=tv(db.now(),12);dt.setTextColor(MUTED);dt.setGravity(Gravity.CENTER);
        dt.setBackground(outline(Color.rgb(248,250,248),14));
        metaRow.addView(dt,new LinearLayout.LayoutParams(0,dp(42),1.15f));
        content.addView(metaRow);space(8);
        if(edit) customer.setText(db.invoiceCustomer(invoiceId));
        section("إدخال الصنف");
        LinearLayout entry=card();entry.setPadding(dp(10),dp(10),dp(10),dp(10));
        LinearLayout line=new LinearLayout(this);
        line.setOrientation(LinearLayout.HORIZONTAL);line.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        EditText total=numberField("الإجمالي");
        EditText qty=numberField("الكمية");
        AutoCompleteTextView item=new AutoCompleteTextView(this);
        item.setHint("اسم الصنف / التفاصيل"); item.setTextSize(13); item.setSingleLine(true); item.setTextColor(TEXT); item.setHintTextColor(MUTED);
        item.setPadding(dp(7),dp(2),dp(7),dp(2)); item.setBackground(outlined(CARD,1,10)); item.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        item.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); item.setTextDirection(View.TEXT_DIRECTION_RTL); item.setSelectAllOnFocus(true);
        item.setOnClickListener(v->item.selectAll());
        item.setOnFocusChangeListener((v,has)->{if(has)item.postDelayed(()->item.selectAll(),60);});
        ArrayList<String> itemSuggestions=new ArrayList<>(Arrays.asList("السمن"));
        Cursor itemCursor=db.items(); while(itemCursor.moveToNext()) itemSuggestions.add(itemCursor.getString(1)); itemCursor.close();
        item.setThreshold(1); item.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,itemSuggestions));
        total.setInputType(2|8192);qty.setInputType(2|8192);qty.setText("1");

        // لا يوجد سعر وحدة في صف الإدخال؛ يُعرض محسوباً داخل صندوق تفاصيل الفاتورة بالأسفل.
        line.addView(total,new LinearLayout.LayoutParams(0,dp(36),1.0f));
        line.addView(qty,new LinearLayout.LayoutParams(0,dp(36),0.72f));
        line.addView(item,new LinearLayout.LayoutParams(0,dp(36),1.35f));
        entry.addView(line);
        Button add=action("＋  إضافة الصنف / التعامل",GREEN);
        entry.addView(add,new LinearLayout.LayoutParams(-1,dp(42)));
        addCard(entry,116);

        section("صندوق عرض الفاتورة");
        LinearLayout invoiceBox=card();
        invoiceBox.setPadding(dp(6),dp(6),dp(6),dp(8));

        // جدول مرتب بخلايا متساوية ومن دون حدود مرئية؛ الأعمدة ثابتة وواضحة بصرياً.
        LinearLayout table=new LinearLayout(this);
        table.setOrientation(LinearLayout.VERTICAL);
        table.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout head=new LinearLayout(this);
        head.setOrientation(LinearLayout.HORIZONTAL);
        head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        String[] heads={"الإجمالي","الكمية","اسم الصنف","سعر الوحدة","حذف"};
        float[] weights={1.0f,.72f,1.35f,.9f,.55f};
        for(int i=0;i<heads.length;i++){
            TextView hv=tv(heads[i],9);
            hv.setTextColor(GREEN);hv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
            hv.setGravity(Gravity.CENTER);hv.setSingleLine(true);
            hv.setBackgroundColor(Color.TRANSPARENT);
            head.addView(hv,new LinearLayout.LayoutParams(0,dp(38),weights[i]));
        }
        table.addView(head,new LinearLayout.LayoutParams(-1,dp(28)));

        ScrollView tableScroll=new ScrollView(this);
        tableScroll.setFillViewport(true);
        tableScroll.setVerticalScrollBarEnabled(true);
        LinearLayout rows=new LinearLayout(this);
        rows.setOrientation(LinearLayout.VERTICAL);
        rows.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        tableScroll.addView(rows,new ViewGroup.LayoutParams(-1,-2));
        table.addView(tableScroll,new LinearLayout.LayoutParams(-1,dp(190))); invoiceBox.addView(table,new LinearLayout.LayoutParams(-1,-2));

        TextView boxTotal=tv("الإجمالي: 0 ريال",20);
        boxTotal.setTextColor(GREEN); boxTotal.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        boxTotal.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        boxTotal.setPadding(dp(10),dp(4),dp(10),dp(4));
        boxTotal.setBackground(bg(Color.rgb(255,249,226),12));
        invoiceBox.addView(boxTotal,new LinearLayout.LayoutParams(-1,dp(48)));
        LinearLayout paidRow=new LinearLayout(this); paidRow.setOrientation(LinearLayout.HORIZONTAL); paidRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL); paidRow.setGravity(Gravity.CENTER_VERTICAL);
        TextView paidTitle=tv("المبلغ المدفوع",12); paidTitle.setTextColor(MUTED); paidTitle.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        EditText paid=numberField("0"); paid.setText("0"); paid.setTextSize(13); paid.setSelectAllOnFocus(true);
        paidRow.addView(paidTitle,new LinearLayout.LayoutParams(0,dp(38),1));
        paidRow.addView(paid,new LinearLayout.LayoutParams(dp(125),dp(38)));
        invoiceBox.addView(paidRow,new LinearLayout.LayoutParams(-1,dp(40)));
        LinearLayout payModes=new LinearLayout(this);payModes.setOrientation(LinearLayout.HORIZONTAL);payModes.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button cashMode=button("نقدي"), creditMode=button("آجل");
        cashMode.setTextColor(GREEN);creditMode.setTextColor(MUTED);
        payModes.addView(cashMode,new LinearLayout.LayoutParams(0,dp(34),1));payModes.addView(creditMode,new LinearLayout.LayoutParams(0,dp(34),1));
        invoiceBox.addView(payModes,new LinearLayout.LayoutParams(-1,dp(36)));
        TextView remainingLabel=tv("المتبقي: 0 ريال",12);remainingLabel.setTextColor(MUTED);remainingLabel.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);remainingLabel.setPadding(dp(10),0,dp(10),0);
        invoiceBox.addView(remainingLabel,new LinearLayout.LayoutParams(-1,dp(28)));
        content.addView(invoiceBox,new LinearLayout.LayoutParams(-1,-2)); space(3);

        final ArrayList<Line> lines=new ArrayList<>();
        if(edit){Cursor c=db.invoiceLines(invoiceId);while(c.moveToNext())lines.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3)));c.close();}

        LinearLayout draftActions=new LinearLayout(this);
        draftActions.setOrientation(LinearLayout.HORIZONTAL);
        Button saveDraft=button("💾 حفظ مؤقت"), restoreDraft=button("↩ استعادة مؤقت");
        saveDraft.setTextColor(GREEN); restoreDraft.setTextColor(GREEN);
        draftActions.addView(saveDraft,new LinearLayout.LayoutParams(0,dp(34),1));
        draftActions.addView(restoreDraft,new LinearLayout.LayoutParams(0,dp(34),1));
        content.addView(draftActions,new LinearLayout.LayoutParams(-1,dp(38))); addSpace(4);
        saveDraft.setOnClickListener(v->saveInvoiceDraft(no.getText().toString(),customer.getText().toString(),paid.getText().toString(),lines));
        
                TextView customerBalance=tv("رصيد العميل: 0",11);
        customerBalance.setTextColor(GREEN);customerBalance.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        customerBalance.setBackground(outline(Color.rgb(241,247,242),8));
        content.addView(customerBalance,new LinearLayout.LayoutParams(-1,dp(30)));addSpace(3);
        TextView paymentMode=tv("نوع السداد: نقدي — ويمكن ترك الباقي آجلًا",10);
        paymentMode.setTextColor(MUTED);paymentMode.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
        content.addView(paymentMode,new LinearLayout.LayoutParams(-1,dp(26)));addSpace(2);
        cashMode.setOnClickListener(v->{paid.setText(fmt(totalOf(lines)));});
        creditMode.setOnClickListener(v->{paid.setText("0");});
        Runnable updateCustomerBalance=()->{
            String cn=customer.getText().toString().trim();
            double cb=cn.isEmpty()?0:db.balanceByName(cn);
            customerBalance.setText("رصيد العميل الحالي: "+balanceText(cb));
            double paidPreview=0;try{paidPreview=Double.parseDouble(paid.getText().toString().trim());}catch(Exception ignored){}
            double invPreview=0;for(Line lx:lines)invPreview+=lx.total;
            double net=cb+invPreview-paidPreview;
            paymentMode.setText("نوع السداد: "+(paidPreview>=invPreview&&invPreview>0?"نقدي":"آجل")+" • بعد الفاتورة: "+balanceText(net));
        };
        customer.setOnItemClickListener((p,v,pos,id)->updateCustomerBalance.run());
        customer.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){updateCustomerBalance.run();}public void afterTextChanged(android.text.Editable e){}});
        final Runnable[] redraw=new Runnable[1];
        redraw[0]=()->{
            rows.removeAllViews();
            double run=0,baseBal=db.balanceByName(customer.getText().toString().trim());
            for(Line l:lines){run+=l.total;addRow(rows,l,run,baseBal,lines);}
            boxTotal.setText("الإجمالي: "+fmt(run)+" ريال");
            double currentBalance=customer.getText().toString().trim().isEmpty()?0:db.balanceByName(customer.getText().toString().trim());
            double paidNow=0; try{paidNow=Double.parseDouble(paid.getText().toString().trim());}catch(Exception ignored){}
            if(paidNow<0)paidNow=0;
            double remaining=currentBalance+run-paidNow;if(Math.abs(remaining)<0.005)remaining=0;
            remainingLabel.setText("المتبقي: "+fmt(remaining)+" ريال"); updateCustomerBalance.run();
        };

        restoreDraft.setOnClickListener(v->restoreInvoiceDraft(no,customer,paid,lines,redraw[0]));
        add.setOnClickListener(v->{
            try{
                double t=Double.parseDouble(total.getText().toString().trim());
                double q=Double.parseDouble(qty.getText().toString().trim());
                String n=item.getText().toString().trim();
                if(n.isEmpty()||q<=0||t<0)throw new Exception();
                lines.add(new Line(n,q,t));
                redraw[0].run();
                total.setText("");qty.setText("1");item.setText("");total.requestFocus();
            }catch(Exception e){
                Toast.makeText(this,"أدخل الإجمالي والكمية واسم الصنف بشكل صحيح",Toast.LENGTH_SHORT).show();
            }
        });

        Button clear=btn("مسح الأصناف");clear.setTextColor(MUTED);
        content.addView(clear,new LinearLayout.LayoutParams(-1,dp(36)));
        clear.setOnClickListener(v->{lines.clear();redraw[0].run();});
        addSpace(6);

        Button save=action(edit?"💾  حفظ التعديل":"💾  حفظ الفاتورة",GREEN);
        content.addView(save,new LinearLayout.LayoutParams(-1,dp(36)));addSpace(6);
        Button print=btn("🖨  طباعة مباشرة — بلوتوث 58mm");print.setTextColor(GREEN);        content.addView(print,new LinearLayout.LayoutParams(-1,dp(38))); addSpace(5);
        

        save.setOnClickListener(v->{
            if(lines.isEmpty()){Toast.makeText(this,"أضف صنفاً واحداً على الأقل",Toast.LENGTH_SHORT).show();return;}
            String cn=customer.getText().toString().trim();
            if(cn.isEmpty()){Toast.makeText(this,"اكتب اسم العميل، أو اتركه للفاتورة النقدية",Toast.LENGTH_SHORT).show();return;}
            String knownPhone=db.phoneByName(cn).trim();
            if(!knownPhone.isEmpty()) saveInvoice(cn,no.getText().toString(),lines,totalOf(lines),parsePaid(paid),knownPhone,edit,invoiceId);
            else showPhoneDialog(cn,no.getText().toString(),lines,totalOf(lines),parsePaid(paid),edit,invoiceId);
        });
        print.setOnClickListener(v->preview(no.getText().toString(),customer.getText().toString(),lines,totalOf(lines),edit,invoiceId));
        item.setOnEditorActionListener((v,a,e)->{add.performClick();return true;});
        customer.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){redraw[0].run();}public void afterTextChanged(android.text.Editable e){}});
        paid.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){redraw[0].run();}public void afterTextChanged(android.text.Editable e){}});
        redraw[0].run();
    }

    double totalOf(ArrayList<Line> ls){double x=0;for(Line l:ls)x+=l.total;return x;}
    double parsePaid(EditText e){try{return Math.max(0,Double.parseDouble(e.getText().toString().trim()));}catch(Exception ex){return 0;}}
    void showPhoneDialog(String name,String no,ArrayList<Line> lines,double total,double paid,boolean edit,long oldId){
        EditText phone=phoneField("رقم هاتف العميل");
        phone.setText(db.phoneByName(name));
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));
        box.addView(tv("رقم العميل غير مسجل. أضف رقم الهاتف حتى يمكن مشاركة الفاتورة معه عبر واتساب. لا يظهر 967 داخل خانة العميل.",12));
        box.addView(phone,new LinearLayout.LayoutParams(-1,dp(40)));
        new AlertDialog.Builder(this).setTitle("إضافة رقم العميل").setView(box)
            .setPositiveButton("حفظ الفاتورة",(d,w)->{
                String p=phone.getText().toString().trim();
                if(p.isEmpty()){Toast.makeText(this,"أدخل رقم العميل حتى يتم حفظه ومشاركة الفاتورة معه.",Toast.LENGTH_SHORT).show();return;}
                saveInvoice(name,no,lines,total,paid,p,edit,oldId);
            }).setNegativeButton("إلغاء",null).show();
    }
    void saveInvoice(String name,String no,ArrayList<Line> lines,double total,double paid,String phone,boolean edit,long oldId){
        if(name==null||name.trim().isEmpty()){Toast.makeText(this,"اختر العميل أو اترك الفاتورة نقدية.",Toast.LENGTH_SHORT).show();return;}
        if(paid<0){Toast.makeText(this,"المبلغ المدفوع غير صحيح.",Toast.LENGTH_SHORT).show();return;}
        long cid=db.customer(name,phone); String date=db.now();
        if(edit){
            String oldNo=db.invoiceNo(oldId);
            db.deleteInvoiceTransactions(oldNo);
            db.updateInvoice(oldId,no,name,total,paid,date);
            db.replaceInvoiceLines(oldId,lines);
        }else{
            long id=db.addInvoice(no,name,total,paid,date);
            db.replaceInvoiceLines(id,lines);
        }
        // القيد المحاسبي الصحيح: الفاتورة تزيد ما على العميل، والدفع ينقصه.
        // إذا زاد الدفع عن قيمة الفاتورة، يتحول الفرق تلقائياً إلى رصيد للعميل.
        if(total>0) db.addTransactionOnce(cid,total,"فاتورة مبيعات رقم "+no,date);
        if(paid>0) db.addPaymentTransaction(cid,paid,"دفعة فاتورة رقم "+no,date);
        cacheLastInvoice(no,name,lines,total,date);
        clearInvoiceDraft();
        saveReceiptImage(no,name,lines,total);
        showPostSaveActions(no,name,lines,total,cid,paid);
    }
    
    void cacheLastInvoice(String no,String customer,ArrayList<Line> lines,double total,String date){
        try{
            File dir=new File(getCacheDir(),"invoices"); if(!dir.exists())dir.mkdirs();
            File file=new File(dir,"last_invoice.txt");
            StringBuilder x=new StringBuilder();
            x.append("رقم الفاتورة: ").append(no).append("\nالعميل: ").append(customer).append("\nالتاريخ: ").append(date).append("\n");
            for(Line l:lines)x.append(l.name).append(" | ").append(fmt(l.qty)).append(" | ").append(fmt(l.total)).append("\n");
            x.append("الإجمالي: ").append(fmt(total)).append(" ريال");
            FileOutputStream out=new FileOutputStream(file,false);out.write(x.toString().getBytes("UTF-8"));out.close();
        }catch(Exception ignored){}
    }
    String b64(String s){return android.util.Base64.encodeToString((s==null?"":s).getBytes(java.nio.charset.StandardCharsets.UTF_8),android.util.Base64.NO_WRAP);}
    String unb64(String s){try{return new String(android.util.Base64.decode(s,android.util.Base64.NO_WRAP),java.nio.charset.StandardCharsets.UTF_8);}catch(Exception e){return "";}}
    void saveInvoiceDraft(String no,String customer,String paid,ArrayList<Line> lines){
        try{
            StringBuilder s=new StringBuilder();
            s.append(b64(no)).append("\n").append(b64(customer)).append("\n").append(b64(paid)).append("\n");
            for(Line l:lines)s.append(b64(l.name)).append("\t").append(l.qty).append("\t").append(l.total).append("\n");
            getSharedPreferences("draft",MODE_PRIVATE).edit().putString("invoice",s.toString()).apply();
            Toast.makeText(this,"تم الحفظ المؤقت ويمكن استعادته لاحقًا",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"تعذر الحفظ المؤقت",Toast.LENGTH_SHORT).show();}
    }
    void restoreInvoiceDraft(TextView no,AutoCompleteTextView customer,EditText paid,ArrayList<Line> lines,Runnable refresh){
        try{
            String s=getSharedPreferences("draft",MODE_PRIVATE).getString("invoice","");
            if(s.isEmpty()){Toast.makeText(this,"لا يوجد حفظ مؤقت",Toast.LENGTH_SHORT).show();return;}
            String[] a=s.split("\n",-1);
            if(a.length<3)throw new Exception();
            no.setText(unb64(a[0]));customer.setText(unb64(a[1]));paid.setText(unb64(a[2]));
            lines.clear();
            for(int i=3;i<a.length;i++){
                if(a[i].trim().isEmpty())continue;
                String[] p=a[i].split("\t",-1);
                if(p.length>=3)lines.add(new Line(unb64(p[0]),Double.parseDouble(p[1]),Double.parseDouble(p[2])));
            }
            refresh.run();Toast.makeText(this,"تم استعادة الحفظ المؤقت",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"الحفظ المؤقت غير صالح",Toast.LENGTH_SHORT).show();}
    }
    void clearInvoiceDraft(){getSharedPreferences("draft",MODE_PRIVATE).edit().remove("invoice").apply();}
    File appDownloadDir(){
        File d=new File(getExternalFilesDir(null),"بقالة العزيز خاص"); if(!d.exists())d.mkdirs(); return d;
    }
    Uri saveReceiptImage(String no,String customer,ArrayList<Line> lines,double total){
        try{
            Bitmap b=receiptBitmap(receiptTextFromLines(no,customer,lines,total,-1));
            String fn="فاتورة_"+no+"_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".png";
            if(Build.VERSION.SDK_INT>=29){
                ContentValues v=new ContentValues();v.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,fn);v.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");v.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/بقالة العزيز خاص");
                Uri u=getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new Exception();
                try(OutputStream out=getContentResolver().openOutputStream(u)){b.compress(Bitmap.CompressFormat.PNG,100,out);}
                return u;
            }else{
                File f=new File(appDownloadDir(),fn);try(FileOutputStream out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}
                return Uri.fromFile(f);
            }
        }catch(Exception e){Toast.makeText(this,"تعذر حفظ صورة الفاتورة",Toast.LENGTH_SHORT).show();return null;}
    }
    void saveAccountStatementImage(long id,String name){
        try{
            String s=statement(id,name);Bitmap b=receiptBitmap(s);
            String fn="كشف_"+name.replaceAll("[\\/:*?\"<>|]","_")+"_"+new SimpleDateFormat("yyyyMMdd_HHmmss",Locale.US).format(new Date())+".png";
            if(Build.VERSION.SDK_INT>=29){
                ContentValues v=new ContentValues();v.put(android.provider.MediaStore.MediaColumns.DISPLAY_NAME,fn);v.put(android.provider.MediaStore.MediaColumns.MIME_TYPE,"image/png");v.put(android.provider.MediaStore.MediaColumns.RELATIVE_PATH,"Download/بقالة العزيز خاص");
                Uri u=getContentResolver().insert(android.provider.MediaStore.Downloads.EXTERNAL_CONTENT_URI,v);if(u==null)throw new Exception();
                try(OutputStream out=getContentResolver().openOutputStream(u)){b.compress(Bitmap.CompressFormat.PNG,100,out);}
            }else{File f=new File(appDownloadDir(),fn);try(FileOutputStream out=new FileOutputStream(f)){b.compress(Bitmap.CompressFormat.PNG,100,out);}}
            Toast.makeText(this,"تم حفظ صورة كشف الحساب في مجلد بقالة العزيز خاص",Toast.LENGTH_SHORT).show();
        }catch(Exception e){Toast.makeText(this,"تعذر حفظ صورة كشف الحساب",Toast.LENGTH_SHORT).show();}
    }
    static void restoreDatabaseFromUri(Context c,Uri uri){
        DB helper=new DB(c);helper.close();
        File target=c.getDatabasePath("enezi.db");File tmp=new File(c.getCacheDir(),"restore_enezi.db");
        try{
            try(InputStream in=c.getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(tmp)){
                if(in==null)throw new Exception("null");byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);
            }
            if(target.exists())target.delete();File wal=new File(target.getPath()+"-wal"),shm=new File(target.getPath()+"-shm");if(wal.exists())wal.delete();if(shm.exists())shm.delete();
            if(!tmp.renameTo(target)){try(InputStream in=new java.io.FileInputStream(tmp);OutputStream out=new FileOutputStream(target)){byte[] buf=new byte[8192];int n;while((n=in.read(buf))>0)out.write(buf,0,n);}}
            tmp.delete();new DB(c).close();
        }catch(Exception e){throw new RuntimeException(e);}
    }
    void showBackupRestore(){
        new AlertDialog.Builder(this).setTitle("النسخ الاحتياطي والاسترجاع")
            .setMessage("النسخة التلقائية: كل يوم الساعة 11:59 مساءً.\\n\\nيمكنك إنشاء نسخة احتياطية يدوياً الآن في أي وقت.\\nالمجلد: Download/بقالة العزيز خاص")
            .setPositiveButton("💾 إنشاء نسخة الآن",(d,w)->{ BackupReceiver.backup(this); Toast.makeText(this,"تم إنشاء النسخة الاحتياطية وحفظها في Download/بقالة العزيز خاص",Toast.LENGTH_LONG).show(); })
            .setNeutralButton("استرجاع نسخة",(d,w)->{Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT);i.setType("*/*");i.addCategory(Intent.CATEGORY_OPENABLE);startActivityForResult(i,8801);})
            .setNegativeButton("إغلاق",null).show();
    }

    void notifyNewOperation(String title,String text){
        try{
            if(android.os.Build.VERSION.SDK_INT>=33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS")!=android.content.pm.PackageManager.PERMISSION_GRANTED){
                requestPermissions(new String[]{"android.permission.POST_NOTIFICATIONS"},7201); return;
            }
            String channelId="operations";
            android.app.NotificationManager nm=(android.app.NotificationManager)getSystemService(NOTIFICATION_SERVICE);
            if(android.os.Build.VERSION.SDK_INT>=26){
                android.app.NotificationChannel ch=new android.app.NotificationChannel(channelId,"إشعارات العمليات",android.app.NotificationManager.IMPORTANCE_DEFAULT);
                ch.setDescription("إشعار عند إضافة فاتورة أو عملية جديدة");nm.createNotificationChannel(ch);
            }
            android.app.Notification.Builder b=android.os.Build.VERSION.SDK_INT>=26?new android.app.Notification.Builder(this,channelId):new android.app.Notification.Builder(this);
            b.setSmallIcon(android.R.drawable.ic_menu_info_details).setContentTitle(title).setContentText(text).setAutoCancel(true);
            nm.notify((int)(System.currentTimeMillis()%100000),b.build());
        }catch(Exception ignored){}
    }

    int dp(int v){return (int)(v*getResources().getDisplayMetrics().density+0.5f);}
    GradientDrawable bg(int color,float radius){return rounded(color,dp((int)radius));}
    GradientDrawable outline(int color,float radius){return outlined(color,1,dp((int)radius));}
    LinearLayout card(){LinearLayout c=new LinearLayout(this);c.setOrientation(LinearLayout.VERTICAL);c.setPadding(dp(8),dp(6),dp(8),dp(6));c.setBackground(outline(CARD,12));c.setElevation(dp(1));return c;}
    void addCard(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(Math.max(50,h-18))));space(4);}
    void add(View v,int h){content.addView(v,new LinearLayout.LayoutParams(-1,dp(Math.max(42,h-12))));space(4);}
    void space(int h){addSpace(dp(h));}
    void spaceInside(LinearLayout p,int h){Space x=new Space(this);p.addView(x,new LinearLayout.LayoutParams(1,dp(h)));}
    Button action(String text,int color){Button b=button(text);b.setTextColor(Color.WHITE);b.setTextSize(16);b.setBackground(rounded(color,dp(14)));return b;}
    Button btn(String text){Button b=button(text);b.setTextColor(TEXT);b.setBackground(outline(CARD,14));return b;}
    static class Line{String name;double qty,total;Line(String n,double q,double t){name=n;qty=q;total=t;}}
    void addRow(LinearLayout parent,Line l,double running,double baseBal,ArrayList<Line> all){
        LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);r.setGravity(Gravity.CENTER_VERTICAL);r.setPadding(dp(2),dp(2),dp(2),dp(2));
        float[] w={1.0f,.72f,1.35f,.9f,.55f};
        TextView total=tv(fmt(l.total),13);total.setGravity(Gravity.CENTER);total.setSingleLine(true);
        TextView qty=tv(fmt(l.qty),13);qty.setGravity(Gravity.CENTER);qty.setSingleLine(true);
        TextView item=tv(l.name,12);item.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);item.setMaxLines(2);item.setEllipsize(TextUtils.TruncateAt.END);
        TextView unit=tv(l.qty==0?"0":fmt(l.total/l.qty),12);unit.setTextColor(MUTED);unit.setGravity(Gravity.CENTER);unit.setSingleLine(true);
        Button del=button("حذف");del.setTextSize(10);del.setTextColor(Color.RED);del.setBackgroundColor(Color.TRANSPARENT);
        total.setBackground(outline(Color.rgb(248,250,248),6));qty.setBackground(outline(Color.rgb(248,250,248),6));item.setBackground(outline(Color.rgb(248,250,248),6));
        total.setContentDescription("تعديل إجمالي الصنف");qty.setContentDescription("تعديل كمية الصنف");item.setContentDescription("تعديل اسم الصنف أو التفاصيل");
        View[] cells={total,qty,item,unit,del};for(int i=0;i<cells.length;i++)r.addView(cells[i],new LinearLayout.LayoutParams(0,dp(36),w[i]));
        total.setOnClickListener(v->editLineTotal(l,parent,all,baseBal));
        qty.setOnClickListener(v->editLineQuantity(l,parent,all,baseBal));
        item.setOnClickListener(v->editLineName(l,parent,all,baseBal));
        del.setOnClickListener(v->{all.remove(l);redrawInvoiceRows(parent,all,baseBal);});
        parent.addView(r,new LinearLayout.LayoutParams(-1,dp(36)));
    }
    void editLineTotal(Line line,LinearLayout parent,ArrayList<Line> all,double baseBal){
        EditText e=numberField("الإجمالي");e.setText(fmt(line.total));e.setSelectAllOnFocus(true);
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(8),dp(4),dp(8),dp(2));box.addView(e,new LinearLayout.LayoutParams(-1,dp(42)));
        new AlertDialog.Builder(this).setTitle("تعديل إجمالي الصنف").setMessage(line.name+" — الإجمالي الحالي: "+fmt(line.total)+" ريال").setView(box)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{try{double value=Double.parseDouble(e.getText().toString().trim());if(value<0)throw new Exception();line.total=value;redrawInvoiceRows(parent,all,baseBal);}catch(Exception ex){Toast.makeText(this,"أدخل إجماليًا صحيحًا",Toast.LENGTH_SHORT).show();}}).show();
    }
    void editLineName(Line line,LinearLayout parent,ArrayList<Line> all,double baseBal){
        EditText e=field("اسم الصنف / التفاصيل");e.setText(line.name);e.setSelectAllOnFocus(true);
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(8),dp(4),dp(8),dp(2));box.addView(e,new LinearLayout.LayoutParams(-1,dp(42)));
        new AlertDialog.Builder(this).setTitle("تعديل اسم الصنف / التفاصيل").setView(box)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{String value=e.getText().toString().trim();if(value.isEmpty()){Toast.makeText(this,"اسم الصنف لا يمكن أن يكون فارغًا",Toast.LENGTH_SHORT).show();return;}line.name=value;redrawInvoiceRows(parent,all,baseBal);}).show();
    }
    void editLineQuantity(Line line,LinearLayout parent,ArrayList<Line> all,double baseBal){
        EditText q=numberField("الكمية");q.setText(fmt(line.qty));q.setSelectAllOnFocus(true);
        LinearLayout box=new LinearLayout(this);box.setPadding(dp(8),dp(4),dp(8),dp(2));box.addView(q,new LinearLayout.LayoutParams(-1,dp(42)));
        new AlertDialog.Builder(this).setTitle("تعديل كمية الصنف").setMessage(line.name+" — الكمية الحالية: "+fmt(line.qty)).setView(box)
            .setNegativeButton("إلغاء",null).setPositiveButton("حفظ",(d,w)->{try{double value=Double.parseDouble(q.getText().toString().trim());if(value<=0)throw new Exception();line.qty=value;redrawInvoiceRows(parent,all,baseBal);}catch(Exception e){Toast.makeText(this,"أدخل كمية صحيحة",Toast.LENGTH_SHORT).show();}}).show();
    }
    void redrawInvoiceRows(LinearLayout parent,ArrayList<Line> all,double baseBal){parent.removeAllViews();double run=0;for(Line x:all){run+=x.total;addRow(parent,x,run,baseBal,all);}}
    void spaceTo(LinearLayout p,int h){Space x=new Space(this);p.addView(x,new LinearLayout.LayoutParams(1,dp(h)));}
    void preview(String no,String customer,ArrayList<Line> lines,double total,boolean edit,long oldId){
        String cleanCustomer=customer==null?"":customer.trim();
        long cid=cleanCustomer.isEmpty()?-1:db.customerIdByName(cleanCustomer);
        double balanceAfter=0;
        if(cid>0){
            balanceAfter=db.balance(cid)+total;
            if(edit){
                String oldCustomer=db.invoiceCustomer(oldId);
                double oldTotal=db.invoiceTotal(oldId);
                if(oldCustomer.equals(cleanCustomer)) balanceAfter-=oldTotal;
            }
            if(Math.abs(balanceAfter)<0.005) balanceAfter=0;
        }
        String s=receiptTextFromLines(no,cleanCustomer,lines,total,cid,balanceAfter);
        TextView v=tv(s,11);v.setTypeface(Typeface.MONOSPACE);v.setGravity(Gravity.CENTER);
        new AlertDialog.Builder(this).setTitle("معاينة إيصال 58mm").setView(v)
            .setPositiveButton("مشاركة واتساب",(d,w)->shareReceiptImageAndText(no,cleanCustomer,lines,total))
            .setNeutralButton("طباعة",(d,w)->printInvoiceBluetooth(no,cleanCustomer,lines,total))
            .setNegativeButton("إغلاق",null).show();
    }

    String receiptTextFromLines(String no,String customer,ArrayList<Line> lines,double total,long cid){
        return receiptTextFromLines(no,customer,lines,total,cid,cid>0?db.balance(cid):0);
    }
    String receiptTextFromLines(String no,String customer,ArrayList<Line> lines,double total,long cid,double balanceAfter){
        StringBuilder s=new StringBuilder();
        s.append("بقالة العزي\nفاتورة ").append(no).append("\n");
        if(customer!=null&&!customer.trim().isEmpty())s.append("العميل: ").append(customer.trim()).append("\n");
        s.append("التاريخ: ").append(db.now()).append("\n\n");
        for(Line l:lines){
            String n=l.name==null?"":l.name.trim();
            s.append(n).append(" × ").append(fmt(l.qty)).append(" = ").append(fmt(l.total)).append(" ريال\n");
        }
        s.append("\nالإجمالي: ").append(fmt(total)).append(" ريال\n");
        if(cid>0&&Math.abs(balanceAfter)>=0.005)s.append(balanceAfter>0?"عليه: ":"له: ").append(fmt(Math.abs(balanceAfter))).append(" ريال\n");
        s.append("شكراً لتعاملكم");
        return s.toString();
    }

    void showPostSaveOperation(String title,String message,Runnable shareAction,Runnable hideAction){
        final Dialog dialog=new Dialog(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(12),dp(16),dp(10));box.setBackground(rounded(CARD,dp(18)));
        TextView t=tv(title,17);t.setTextColor(GREEN);t.setTypeface(Typeface.DEFAULT,Typeface.BOLD);t.setGravity(Gravity.CENTER);box.addView(t,new LinearLayout.LayoutParams(-1,dp(34)));
        TextView m=tv(message,12);m.setTextColor(TEXT);m.setGravity(Gravity.CENTER);m.setMaxLines(5);fitInside(m,12f,9f);box.addView(m,new LinearLayout.LayoutParams(-1,dp(78)));
        LinearLayout actions=new LinearLayout(this);actions.setOrientation(LinearLayout.HORIZONTAL);
        Button share=button("📤 مشاركة");share.setTextColor(Color.WHITE);share.setBackgroundColor(GREEN);Button hide=button("إخفاء");hide.setTextColor(MUTED);
        actions.addView(share,new LinearLayout.LayoutParams(0,dp(40),1));actions.addView(hide,new LinearLayout.LayoutParams(0,dp(40),1));box.addView(actions);
        share.setOnClickListener(v->{dialog.dismiss();shareAction.run();});hide.setOnClickListener(v->{dialog.dismiss();hideAction.run();});
        dialog.setContentView(box);dialog.setCanceledOnTouchOutside(false);dialog.show();
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(dp(330),WindowManager.LayoutParams.WRAP_CONTENT);dialog.getWindow().setGravity(Gravity.CENTER);}
    }

    void showPostSaveActions(String no,String customer,ArrayList<Line> lines,double total,long cid,double paid){
        String status=paid>=total?"مسددة":(paid>0?"متبقي "+fmt(total-paid)+" ريال":"غير مسددة");
        int statusColor=paid>=total?BLUE:RED;
        final Dialog dialog=new Dialog(this);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(16),dp(12),dp(16),dp(10));box.setBackground(rounded(CARD,dp(18)));
        TextView title=tv("تم حفظ الفاتورة بنجاح",17);title.setTextColor(GREEN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER);
        box.addView(title,new LinearLayout.LayoutParams(-1,dp(36)));
        TextView sub=tv("الفاتورة: "+no+"\nالإجمالي: "+fmt(total)+" ريال\nالمدفوع: "+fmt(paid)+" ريال",12);sub.setTextColor(TEXT);sub.setGravity(Gravity.CENTER);
        box.addView(sub,new LinearLayout.LayoutParams(-1,dp(62)));
        TextView statusV=tv(status,14);statusV.setTextColor(statusColor);statusV.setTypeface(Typeface.DEFAULT,Typeface.BOLD);statusV.setGravity(Gravity.CENTER);
        box.addView(statusV,new LinearLayout.LayoutParams(-1,dp(28)));
        double currentBalance=db.balance(cid);TextView bal=tv("الرصيد بعد الفاتورة: "+balanceText(currentBalance),12);bal.setTextColor(balanceColor(currentBalance));bal.setGravity(Gravity.CENTER);
        box.addView(bal,new LinearLayout.LayoutParams(-1,dp(30)));

        LinearLayout actions1=new LinearLayout(this);actions1.setOrientation(LinearLayout.HORIZONTAL);actions1.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button share=button("📤 مشاركة");share.setTextColor(Color.WHITE);share.setBackgroundColor(GREEN);
        Button newInvoice=button("＋ فاتورة جديدة");newInvoice.setTextColor(GREEN);newInvoice.setBackground(outline(Color.rgb(241,247,242),10));
        actions1.addView(share,new LinearLayout.LayoutParams(0,dp(42),1));
        actions1.addView(newInvoice,new LinearLayout.LayoutParams(0,dp(42),1));
        box.addView(actions1);

        Button close=button("✓ إقفال الفاتورة والعودة للسجل");close.setTextColor(MUTED);close.setBackground(outline(CARD,10));
        box.addView(close,new LinearLayout.LayoutParams(-1,dp(40)));

        share.setOnClickListener(v->{
            dialog.dismiss();
            // الانتقال للسجل أولاً يمنع الرجوع إلى شاشة الفاتورة المحفوظة بعد العودة من واتساب.
            invoiceHistory();
            shareReceiptImageAndText(no,customer,lines,total);
        });
        newInvoice.setOnClickListener(v->{dialog.dismiss();invoice();});
        close.setOnClickListener(v->{dialog.dismiss();invoiceHistory();});

        dialog.setContentView(box);dialog.setCanceledOnTouchOutside(false);dialog.setCancelable(false);dialog.show();
        if(dialog.getWindow()!=null){dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);dialog.getWindow().setLayout(dp(340),WindowManager.LayoutParams.WRAP_CONTENT);dialog.getWindow().setGravity(Gravity.CENTER);}
    }

    void showInvoiceDialog(long id,String no,String customer,double total,String date){
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(10),dp(5),dp(10),dp(5));
        TextView head=tv("فاتورة رقم "+no,18); head.setTextColor(GREEN); head.setTypeface(Typeface.DEFAULT,Typeface.BOLD); box.addView(head,new LinearLayout.LayoutParams(-1,dp(38)));
        box.addView(tv("العميل: "+(customer==null||customer.isEmpty()?"نقدي":customer)+"\nالتاريخ والوقت: "+date,12),new LinearLayout.LayoutParams(-1,dp(50)));
        sectionInside(box,"الأصناف");
        Cursor c=db.invoiceLines(id); int count=0;
        while(c.moveToNext()){
            String n=c.getString(1); double q=c.getDouble(2), t=c.getDouble(3);
            TextView row=tv(n+"  ×  "+fmt(q)+"  =  "+fmt(t)+" ريال",12);
            row.setBackground(outline(Color.rgb(248,250,248),8)); box.addView(row,new LinearLayout.LayoutParams(-1,dp(34))); count++;
        }
        c.close();
        if(count==0) box.addView(tv("لا توجد تفاصيل أصناف محفوظة لهذه الفاتورة.",11),new LinearLayout.LayoutParams(-1,dp(34)));
        TextView totalV=tv("الإجمالي: "+fmt(total)+" ريال",17); totalV.setTextColor(GREEN); totalV.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        box.addView(totalV,new LinearLayout.LayoutParams(-1,dp(42)));
        AlertDialog dialog=new AlertDialog.Builder(this).setView(box)
            .setPositiveButton("تعديل",null).setNegativeButton("إغلاق",null).create();
        dialog.setOnShowListener(x->dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{dialog.dismiss();invoice(true,id);}));
        dialog.show();
    }

    void showOperationDetails(String customer,long tid,String details,double amount,int type){
        String inv=db.invoiceNoFromTransaction(details);
        if(!inv.isEmpty()){
            long iid=db.invoiceIdByNo(inv);
            if(iid>0){Cursor c=db.invoiceLines(iid); ArrayList<Line> ls=new ArrayList<>(); while(c.moveToNext())ls.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3))); c.close();
                new AlertDialog.Builder(this).setTitle("تفاصيل العملية")
                    .setMessage("العميل: "+customer+"\nالفاتورة: "+inv+"\nالإجمالي: "+fmt(amount)+" ريال\n"+(ls.isEmpty()?"":db.invoiceCompactDetails(inv)))
                    .setPositiveButton("إغلاق",null).show(); return;
            }
        }
        new AlertDialog.Builder(this).setTitle("تفاصيل العملية")
            .setMessage("العميل: "+customer+"\n"+(details==null?"عملية مالية":details)+"\n"+(type==1?"عليه: ":"له: ")+fmt(amount)+" ريال")
            .setPositiveButton("إغلاق",null).show();
    }

    void sectionInside(LinearLayout box,String title){
        TextView v=tv(title,11);v.setTextColor(GREEN);v.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(v,new LinearLayout.LayoutParams(-1,dp(26)));
    }

    void invoiceHistory(){
        base("الفواتير"); section("سجل الفواتير");
        Cursor c=db.invoices();
        while(c.moveToNext()){
            long id=c.getLong(0); String no=c.getString(1),cn=c.getString(2),date=c.getString(4); double total=c.getDouble(3);
            LinearLayout r=card(); r.setPadding(dp(7),dp(3),dp(7),dp(3));
            TextView info=tv("فاتورة "+no+"  •  "+(cn==null||cn.isEmpty()?"نقدي":cn)+"  •  "+fmt(total)+" ريال\n"+date,12);
            info.setMaxLines(3); info.setEllipsize(null); info.setIncludeFontPadding(true);
            r.addView(info,new LinearLayout.LayoutParams(-1,dp(40)));
            r.setOnClickListener(v->showInvoiceDialog(id,no,cn,total,date));
            LinearLayout a=new LinearLayout(this); a.setOrientation(LinearLayout.HORIZONTAL);
            Button edit=button("تعديل"),del=button("حذف"); edit.setTextColor(GREEN);del.setTextColor(Color.RED);
            a.addView(edit,new LinearLayout.LayoutParams(0,dp(32),1));a.addView(del,new LinearLayout.LayoutParams(0,dp(32),1));r.addView(a);
            edit.setOnClickListener(v->invoice(true,id));
            del.setOnClickListener(v->new AlertDialog.Builder(this).setTitle("حذف الفاتورة؟").setMessage("سيتم حذف الفاتورة وحركتها من حساب العميل.").setPositiveButton("حذف",(d,w)->{db.deleteInvoice(id);invoiceHistory();}).setNegativeButton("إلغاء",null).show());
            content.addView(r,new LinearLayout.LayoutParams(-1,dp(78))); addSpace(3);
        } c.close();
    }
    void showPurchaseInvoiceDialog(long id,String no,String supplier,double total,String date){
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(10),dp(5),dp(10),dp(5));
        TextView head=tv("فاتورة شراء رقم "+no,18);head.setTextColor(GOLD);head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);head.setGravity(Gravity.CENTER);box.addView(head,new LinearLayout.LayoutParams(-1,dp(38)));
        box.addView(tv("المورد: "+(supplier==null||supplier.isEmpty()?"بدون مورد":supplier)+"\nالتاريخ والوقت: "+date,12),new LinearLayout.LayoutParams(-1,dp(50)));
        sectionInside(box,"الأصناف");
        Cursor c=db.purchaseLines(id);int count=0;
        while(c.moveToNext()){
            String n=c.getString(1);double q=c.getDouble(2),cost=c.getDouble(3),sale=c.getDouble(4),t=c.getDouble(5);
            TextView row=tv(n+"  ×  "+fmt(q)+"  |  تكلفة الوحدة: "+fmt(cost)+"  |  البيع: "+fmt(sale)+"  |  "+fmt(t)+" ريال",10);
            row.setBackground(outline(Color.rgb(248,250,248),8));row.setMaxLines(3);row.setEllipsize(null);box.addView(row,new LinearLayout.LayoutParams(-1,dp(42)));count++;
        }
        c.close();if(count==0)box.addView(tv("لا توجد تفاصيل أصناف محفوظة لهذه الفاتورة.",11),new LinearLayout.LayoutParams(-1,dp(34)));
        TextView totalV=tv("الإجمالي: "+fmt(total)+" ريال",17);totalV.setTextColor(GREEN);totalV.setTypeface(Typeface.DEFAULT,Typeface.BOLD);box.addView(totalV,new LinearLayout.LayoutParams(-1,dp(42)));
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("فاتورة الشراء").setView(box)
            .setPositiveButton("مشاركة",null).setNeutralButton("طباعة",null).setNegativeButton("إغلاق",null).create();
        dialog.setOnShowListener(x->{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{dialog.dismiss();sharePurchaseInvoice(id,no,supplier,total,date);});
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(v->{dialog.dismiss();printPurchaseInvoice(id,no,supplier,total,date);});
        });
        dialog.show();
    }

    ArrayList<PurchaseLine> loadPurchaseLines(long id){
        ArrayList<PurchaseLine> ls=new ArrayList<>();Cursor c=db.purchaseLines(id);
        while(c.moveToNext())ls.add(new PurchaseLine(c.getString(1),c.getDouble(2),c.getDouble(5),c.getDouble(3),c.getDouble(4)));
        c.close();return ls;
    }
    String purchaseReceiptText(String no,String supplier,ArrayList<PurchaseLine> lines,double total,String date){
        // إيصال شراء متوافق مع طابعة حرارية Bluetooth بعرض 58mm: ثلاثة أعمدة فقط،
        // مع إبقاء تكلفة الوحدة وسعر البيع في سطر منفصل لتجنب قصّ النص أو تداخل الأعمدة.
        StringBuilder s=new StringBuilder("بقالة العزي\\nفاتورة شراء رقم: ").append(no).append("\\nالمورد: ").append(supplier==null||supplier.isEmpty()?"بدون مورد":supplier).append("\\nالتاريخ: ").append(date).append("\\n");
        s.append("------------------------------\\nالصنف | الكمية | الإجمالي\\n");
        for(PurchaseLine l:lines){
            String n=l.name==null?"":l.name.trim();
            s.append(n).append(" | ").append(fmt(l.qty)).append(" | ").append(fmt(l.total)).append("\\n");
            s.append("تكلفة: ").append(fmt(l.cost)).append("  •  بيع: ").append(fmt(l.sale)).append(" ريال\\n");
        }
        s.append("------------------------------\\nالإجمالي: ").append(fmt(total)).append(" ريال\\nشكراً لتعاملكم معنا");
        return s.toString();
    }
    void sharePurchaseInvoice(long id,String no,String supplier,double total,String date){
        try{
            String text=purchaseReceiptText(no,supplier,loadPurchaseLines(id),total,date);
            String phone=db.supplierPhoneByName(supplier);
            shareWhatsAppToCustomer(phone,text,null);
        }catch(Exception e){shareText(purchaseReceiptText(no,supplier,loadPurchaseLines(id),total,date));}
    }
    void printPurchaseInvoice(long id,String no,String supplier,double total,String date){
        try{printTextBluetooth(purchaseReceiptText(no,supplier,loadPurchaseLines(id),total,date));}
        catch(Exception e){Toast.makeText(this,"تعذر تجهيز فاتورة الشراء للطباعة",Toast.LENGTH_LONG).show();}
    }

    String receiptText(String no,String customer,LinearLayout rows,double total,long cid){StringBuilder s=new StringBuilder("بقالة العزي\nفاتورة رقم: ").append(no).append("\nالتاريخ: ").append(db.now()).append("\n");if(!customer.isEmpty())s.append("العميل: ").append(customer).append("\n");s.append("------------------------------\n");for(int i=0;i<rows.getChildCount();i++){View ch=rows.getChildAt(i);if(ch instanceof LinearLayout){LinearLayout r=(LinearLayout)ch;StringBuilder q=new StringBuilder();for(int j=0;j<r.getChildCount();j++){View x=r.getChildAt(j);if(x instanceof TextView){String z=((TextView)x).getText().toString().trim();if(!z.isEmpty()){if(q.length()>0)q.append(" | ");q.append(z);}}}if(q.length()>0)s.append(q).append("\n");}}s.append("------------------------------\nالإجمالي: ").append(fmt(total)).append(" ريال\n");if(cid>0)s.append(balanceText(db.balance(cid))).append("\n");s.append("شكراً لتعاملكم معنا");return s.toString();}
    int balanceColor(double balance){if(balance>0.005)return RED;if(balance<-0.005)return BLUE;return GREEN;}
    String balanceText(double b){double x=Math.abs(b)<0.005?0:b;if(x>0)return "رصيدكم عليكم: "+fmt(x)+" ريال";if(x<0)return "رصيدكم لكم: "+fmt(Math.abs(x))+" ريال";return "رصيدكم عليكم: 0 ريال";}
    File createA4Pdf(String text,String prefix){
        File dir=new File(getCacheDir(),"pdf");if(!dir.exists())dir.mkdirs();File file=new File(dir,prefix+"_"+System.currentTimeMillis()+".pdf");
        android.graphics.pdf.PdfDocument pdf=new android.graphics.pdf.PdfDocument();
        final int pageW=595,pageH=842,margin=42,contentW=pageW-(margin*2);
        TextPaint paint=new TextPaint(Paint.ANTI_ALIAS_FLAG);paint.setColor(Color.BLACK);paint.setTextSize(dp(10));paint.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        int pageNo=1;android.graphics.pdf.PdfDocument.Page page=null;Canvas canvas=null;int y=margin;
        try{
            for(String line:text.split("\\n",-1)){
                String safe=line==null?"":line;
                if(page==null){page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW,pageH,pageNo).create());canvas=page.getCanvas();y=margin;}
                StaticLayout layout;
                if(Build.VERSION.SDK_INT>=23) layout=StaticLayout.Builder.obtain(safe,0,safe.length(),paint,contentW).setAlignment(Layout.Alignment.ALIGN_OPPOSITE).setIncludePad(false).setLineSpacing(1.0f,0.0f).setTextDirection(android.text.TextDirectionHeuristics.RTL).build();
                else layout=new StaticLayout(safe,paint,contentW,Layout.Alignment.ALIGN_OPPOSITE,1.0f,2,true);
                if(y+layout.getHeight()>pageH-margin){pdf.finishPage(page);pageNo++;page=pdf.startPage(new android.graphics.pdf.PdfDocument.PageInfo.Builder(pageW,pageH,pageNo).create());canvas=page.getCanvas();y=margin;}
                canvas.save();canvas.translate(margin,y);layout.draw(canvas);canvas.restore();y+=layout.getHeight()+5;
            }
            if(page!=null)pdf.finishPage(page);try(FileOutputStream out=new FileOutputStream(file)){pdf.writeTo(out);}catch(java.io.IOException e){throw new RuntimeException(e);}return file;
        }finally{pdf.close();}
    }
    void shareAccountPdfToWhatsApp(long id,String name){
        try{
            File file=createA4Pdf(statement(id,name),"كشف_حساب_"+id);
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent i=new Intent(Intent.ACTION_SEND);i.setType("application/pdf");
            i.putExtra(Intent.EXTRA_STREAM,uri);
            i.putExtra(Intent.EXTRA_TEXT,"كشف حساب "+name);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            String p=normalizeWhatsAppPhone(db.phoneByName(name));if(!p.isEmpty())i.putExtra("jid",p+"@s.whatsapp.net");
            try{i.setPackage("com.whatsapp");startActivity(i);}catch(Exception e){i.setPackage(null);startActivity(Intent.createChooser(i,"مشاركة كشف الحساب PDF"));}
        }catch(Exception e){Toast.makeText(this,"تعذر إنشاء كشف الحساب PDF",Toast.LENGTH_LONG).show();}
    }

    void shareText(String s){Intent i=new Intent(Intent.ACTION_SEND);i.setType("text/plain");i.putExtra(Intent.EXTRA_TEXT,s);startActivity(Intent.createChooser(i,"إرسال الفاتورة"));}
    String normalizeWhatsAppPhone(String phone){
        String p=phone==null?"":phone.replaceAll("[^0-9+]","");
        if(p.startsWith("+"))p=p.substring(1);
        if(p.startsWith("00"))p=p.substring(2);
        if(p.startsWith("0")&&p.length()>=8)p="967"+p.substring(1);
        else if(p.matches("\\d{9}"))p="967"+p;
        return p;
    }
    void shareWhatsAppToCustomer(String phone,String text,Uri image){
        String p=normalizeWhatsAppPhone(phone);
        Intent i=new Intent(Intent.ACTION_SEND);i.setType(image!=null?"image/png":"text/plain");
        i.putExtra(Intent.EXTRA_TEXT,text);
        if(image!=null){i.putExtra(Intent.EXTRA_STREAM,image);i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);}
        if(!p.isEmpty())i.putExtra("jid",p+"@s.whatsapp.net");
        try{
            i.setPackage("com.whatsapp");startActivity(i);
        }catch(Exception e){
            try{
                Intent chat=new Intent(Intent.ACTION_VIEW,Uri.parse("https://wa.me/"+p+"?text="+Uri.encode(text)));
                chat.setPackage("com.whatsapp");startActivity(chat);
            }catch(Exception ignored){
                i.setPackage(null);startActivity(Intent.createChooser(i,"مشاركة الفاتورة"));
            }
        }
    }
    Bitmap receiptBitmap(String text){
        final int width=384;
        final int margin=16;
        final int black=Color.BLACK;
        final int gray=Color.rgb(90,90,90);
        final int green=Color.rgb(24,112,61);
        final int lineH=24;
        String[] ls=text.split("\\n",-1);
        int rows=0;
        for(String s:ls) if(s.contains(" | ")) rows++;
        int height=150+rows*lineH+ls.length*16;
        Bitmap b=Bitmap.createBitmap(width,Math.max(240,height),Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(b);canvas.drawColor(Color.WHITE);

        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));
        p.setColor(black);
        p.setTextAlign(Paint.Align.CENTER);

        // شعار التطبيق الحقيقي في أعلى الإيصال.
        try{
            Drawable d=getResources().getDrawable(com.saleh.enezi.R.drawable.ic_store);
            int size=54;
            d.setBounds((width-size)/2,6,(width+size)/2,6+size);
            d.draw(canvas);
        }catch(Exception ignored){}

        p.setTypeface(Typeface.create("sans",Typeface.BOLD));
        p.setTextSize(18);p.setColor(green);canvas.drawText("بقالة العزي",width/2,82,p);
        p.setTextSize(14);p.setColor(black);canvas.drawText("فاتورة مبيعات",width/2,103,p);

        int y=124;
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(10);p.setColor(gray);
        for(String line:ls){
            if(line.equals("بقالة العزي")) continue;
            if(line.startsWith("فاتورة مبيعات رقم:")){
                p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(10);p.setColor(gray);
                canvas.drawText(line,width/2,y,p);y+=17;continue;
            }
            if(line.equals("------------------------------")) continue;
            if(line.equals("الصنف | الكمية | الإجمالي")){
                y+=5;
                p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(11);p.setColor(green);
                canvas.drawText("الصنف",125,y,p);canvas.drawText("الكمية",250,y,p);canvas.drawText("الإجمالي",335,y,p);
                y+=16;
                canvas.drawLine(margin,y,width-margin,y,p); y+=14;
                p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(11);p.setColor(black);
                continue;
            }
            if(line.contains(" | ")){
                String[] q=line.split(" \\| ",-1);
                if(q.length>=3){
                    String item=q[0].trim();
                    if(item.length()>17)item=item.substring(0,17)+"…";
                    p.setTextAlign(Paint.Align.RIGHT);canvas.drawText(item,215,y,p);
                    p.setTextAlign(Paint.Align.CENTER);canvas.drawText(q[1],250,y,p);
                    p.setTextAlign(Paint.Align.RIGHT);canvas.drawText(q[2],365,y,p);
                    p.setTextAlign(Paint.Align.CENTER);y+=lineH;continue;
                }
            }
            p.setTextAlign(Paint.Align.CENTER);
            if(line.startsWith("التاريخ:")||line.startsWith("العميل:")){p.setColor(gray);canvas.drawText(line,width/2,y,p);y+=17;continue;}
            if(line.startsWith("الإجمالي:")||line.startsWith("المتبقي")){
                y+=5;p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(14);p.setColor(green);
                canvas.drawText(line,width/2,y,p);y+=21;p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(11);p.setColor(black);continue;
            }
            if(line.startsWith("شكراً")){y+=7;p.setTextSize(10);p.setColor(gray);canvas.drawText(line,width/2,y,p);y+=18;continue;}
            p.setColor(black);canvas.drawText(line,width/2,y,p);y+=17;
        }
        return Bitmap.createBitmap(b,0,0,width,Math.min(y+12,b.getHeight()));
    }
    Uri saveReceiptBitmap(Bitmap bitmap,String no)throws Exception{File dir=new File(getCacheDir(),"receipts");if(!dir.exists())dir.mkdirs();File file=new File(dir,"invoice_"+no+"_"+System.currentTimeMillis()+".png");FileOutputStream out=new FileOutputStream(file);bitmap.compress(Bitmap.CompressFormat.PNG,100,out);out.close();return FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);}
    void shareReceiptImageAndText(String no,String customer,ArrayList<Line> lines,double total){try{long cid=customer==null||customer.trim().isEmpty()?-1:db.customerIdByName(customer.trim());String text=receiptTextFromLines(no,customer,lines,total,cid);Uri uri=saveReceiptBitmap(receiptBitmap(text),no);String phone=db.phoneByName(customer);shareWhatsAppToCustomer(phone,text,uri);}catch(Exception e){shareText(receiptTextFromLines(no,customer,lines,total,customer==null||customer.isEmpty()?-1:db.customerIdByName(customer)));}}
    String pendingPrintNo="",pendingPrintCustomer="";ArrayList<Line> pendingPrintLines;double pendingPrintTotal;
    String pendingPrintText="";
    void printTextBluetooth(String text){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED){
            pendingPrintText=text;requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},5102);return;
        }
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();
        if(adapter==null){Toast.makeText(this,"هذا الجهاز لا يدعم البلوتوث",Toast.LENGTH_LONG).show();return;}
        if(!adapter.isEnabled()){Toast.makeText(this,"فعّل البلوتوث ثم أعد الضغط على الطباعة",Toast.LENGTH_LONG).show();return;}
        Set<BluetoothDevice> paired=adapter.getBondedDevices();
        if(paired==null||paired.isEmpty()){Toast.makeText(this,"لا توجد طابعة مقترنة. اقترن بالطابعة من إعدادات البلوتوث أولاً.",Toast.LENGTH_LONG).show();return;}
        BluetoothDevice[] devices=paired.toArray(new BluetoothDevice[0]);String[] names=new String[devices.length];
        for(int i=0;i<devices.length;i++)names[i]=(devices[i].getName()==null?"طابعة بلوتوث":devices[i].getName())+"\n"+devices[i].getAddress();
        new AlertDialog.Builder(this).setTitle("اختر طابعة 58mm").setItems(names,(d,w)->{
            Bitmap bitmap=receiptBitmap(text);
            new Thread(()->sendBitmapToBluetooth(devices[w],bitmap)).start();
        }).setNegativeButton("إلغاء",null).show();
    }
    void sendBitmapToBluetooth(BluetoothDevice device,Bitmap bitmap){
        BluetoothSocket socket=null;OutputStream out=null;
        try{
            UUID spp=UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");
            try{
                socket=device.createRfcommSocketToServiceRecord(spp);
                socket.connect();
            }catch(Exception first){
                try{if(socket!=null)socket.close();}catch(Exception ignored){}
                socket=device.createInsecureRfcommSocketToServiceRecord(spp);
                socket.connect();
            }
            out=socket.getOutputStream();
            out.write(new byte[]{0x1B,0x40});                 // تهيئة الطابعة
            out.write(rasterBytes(bitmap));                  // 384px = 58mm على أغلب طابعات 203dpi
            out.write(new byte[]{0x1B,0x64,0x04});            // تغذية الورق
            out.write(new byte[]{0x1D,0x56,0x00});            // قص إن كانت الطابعة تدعم القص
            out.flush();
            runOnUiThread(()->Toast.makeText(this,"تم إرسال العملية إلى الطابعة 58mm",Toast.LENGTH_SHORT).show());
        }catch(Exception e){
            runOnUiThread(()->Toast.makeText(this,"تعذر الطباعة. تأكد من اقتران طابعة 58mm وأنها جاهزة للورق.",Toast.LENGTH_LONG).show());
        }finally{
            try{if(out!=null)out.close();}catch(Exception ignored){}
            try{if(socket!=null)socket.close();}catch(Exception ignored){}
        }
    }
    void printInvoiceBluetooth(String no,String customer,ArrayList<Line> lines,double total){
        if(Build.VERSION.SDK_INT>=31&&checkSelfPermission("android.permission.BLUETOOTH_CONNECT")!=PackageManager.PERMISSION_GRANTED){pendingPrintNo=no;pendingPrintCustomer=customer;pendingPrintLines=new ArrayList<>(lines);pendingPrintTotal=total;requestPermissions(new String[]{"android.permission.BLUETOOTH_CONNECT"},5101);return;}
        BluetoothAdapter adapter=BluetoothAdapter.getDefaultAdapter();if(adapter==null){Toast.makeText(this,"هذا الجهاز لا يدعم البلوتوث",Toast.LENGTH_LONG).show();return;}if(!adapter.isEnabled()){Toast.makeText(this,"فعّل البلوتوث ثم أعد الضغط على الطباعة",Toast.LENGTH_LONG).show();return;}
        Set<BluetoothDevice> paired=adapter.getBondedDevices();if(paired==null||paired.isEmpty()){Toast.makeText(this,"لا توجد طابعة مقترنة. اقترن بالطابعة من إعدادات البلوتوث أولاً.",Toast.LENGTH_LONG).show();return;}
        BluetoothDevice[] devices=paired.toArray(new BluetoothDevice[0]);String[] names=new String[devices.length];for(int i=0;i<devices.length;i++)names[i]=(devices[i].getName()==null?"طابعة بلوتوث":devices[i].getName())+"\n"+devices[i].getAddress();
        new AlertDialog.Builder(this).setTitle("اختر طابعة 58mm").setItems(names,(d,w)->printToBluetooth(devices[w],no,customer,lines,total)).setNegativeButton("إلغاء",null).show();
    }
    void printToBluetooth(BluetoothDevice device,String no,String customer,ArrayList<Line> lines,double total){
        long cid=customer==null||customer.trim().isEmpty()?-1:db.customerIdByName(customer.trim());
        String text=receiptTextFromLines(no,customer,lines,total,cid);
        Bitmap bitmap=receiptBitmap(text);
        new Thread(()->{
            sendBitmapToBluetooth(device,bitmap);
            runOnUiThread(()->Toast.makeText(this,"تمت معالجة فاتورة 58mm",Toast.LENGTH_SHORT).show());
        }).start();
    }
    byte[] rasterBytes(Bitmap bitmap){int width=bitmap.getWidth(),height=bitmap.getHeight(),bpr=(width+7)/8;byte[] out=new byte[8+bpr*height];out[0]=0x1D;out[1]=0x76;out[2]=0x30;out[3]=0;out[4]=(byte)(bpr&255);out[5]=(byte)((bpr>>8)&255);out[6]=(byte)(height&255);out[7]=(byte)((height>>8)&255);int p=8;for(int y=0;y<height;y++)for(int xb=0;xb<bpr;xb++){int v=0;for(int bit=0;bit<8;bit++){int x=xb*8+bit;if(x<width){int px=bitmap.getPixel(x,y);int g=(Color.red(px)+Color.green(px)+Color.blue(px))/3;if(g<180)v|=1<<(7-bit);}}out[p++]=(byte)v;}return out;}
    @Override public void onRequestPermissionsResult(int requestCode,String[] permissions,int[] grantResults){
        super.onRequestPermissionsResult(requestCode,permissions,grantResults);
        if(requestCode==REQ_CONTACTS){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED)importContact();
            else Toast.makeText(this,"يلزم السماح بالوصول إلى جهات الاتصال",Toast.LENGTH_LONG).show();
        }else if(requestCode==REQ_PERM_CAMERA){
            if(grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED) launchScanCamera();
            else Toast.makeText(this,"يلزم السماح بالوصول إلى الكاميرا لالتقاط الفاتورة",Toast.LENGTH_SHORT).show();
        }else if(requestCode==5101&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&pendingPrintLines!=null){
            printInvoiceBluetooth(pendingPrintNo,pendingPrintCustomer,pendingPrintLines,pendingPrintTotal);
        }else if(requestCode==5102&&grantResults.length>0&&grantResults[0]==PackageManager.PERMISSION_GRANTED&&!pendingPrintText.isEmpty()){
            String x=pendingPrintText;pendingPrintText="";printTextBluetooth(x);
        }
    }
    void thermalPreview(String no,String customer,LinearLayout rows,double total){preview(no,customer,new ArrayList<Line>(),total,false,-1);}
    static String fmt(double x){return String.format(Locale.US,"%.2f",x).replace(".00","");}

    void customers(){
        base("الحسابات"); section("حسابات العملاء");
        EditText search=field("بحث باسم العميل"); addField(search);
        LinearLayout addBox=new LinearLayout(this); addBox.setOrientation(LinearLayout.VERTICAL);
        addBox.setPadding(dp(12),dp(10),dp(12),dp(10)); addBox.setBackground(outlined(CARD,1,16));
        TextView addTitle=tv("إضافة عميل جديد",17); addTitle.setTextColor(GREEN); addTitle.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        addBox.addView(addTitle,new LinearLayout.LayoutParams(-1,dp(30)));
        EditText name=field("اسم العميل"), phone=phoneField("رقم الهاتف");
        customerNameInput=name;customerPhoneInput=phone;
        LinearLayout customerFields=new LinearLayout(this);customerFields.setOrientation(LinearLayout.HORIZONTAL);customerFields.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        customerFields.addView(name,new LinearLayout.LayoutParams(0,dp(36),1.35f));customerFields.addView(phone,new LinearLayout.LayoutParams(0,dp(36),1f));addBox.addView(customerFields);
        addBox.addView(new Space(this),new LinearLayout.LayoutParams(1,dp(8)));
        LinearLayout contactActions=new LinearLayout(this);contactActions.setOrientation(LinearLayout.HORIZONTAL);
        Button pick=button("👤 جهات الاتصال"); pick.setTextColor(GREEN); pick.setOnClickListener(v->importContact());
        Button add=button("＋ إضافة العميل"); add.setTextColor(Color.WHITE); add.setBackgroundColor(GREEN);
        contactActions.addView(pick,new LinearLayout.LayoutParams(0,dp(36),1));contactActions.addView(add,new LinearLayout.LayoutParams(0,dp(36),1));addBox.addView(contactActions);
        content.addView(addBox,new LinearLayout.LayoutParams(-1,-2)); addSpace(12);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        final Runnable[] refresh={null};
        refresh[0]=()->{
            list.removeAllViews();Cursor c=db.customers(search.getText().toString());
            while(c.moveToNext()){
                long id=c.getLong(0);String n=c.getString(1);double bal=db.balance(id);
                LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(8),dp(4),dp(8),dp(4));row.setBackground(outlined(CARD,1,12));
                row.setOnClickListener(v->account(id,n));row.setOnLongClickListener(v->{customerActions(id,n);return true;});
                TextView balance=tv(balanceText(bal),13);balance.setTextColor(bal>0?Color.rgb(190,55,45):GREEN);balance.setTypeface(Typeface.DEFAULT,Typeface.BOLD);balance.setGravity(Gravity.CENTER);balance.setMaxLines(2);balance.setEllipsize(null);
                TextView title=tv(n,15);title.setTextColor(GREEN);title.setTypeface(Typeface.DEFAULT,Typeface.BOLD);title.setGravity(Gravity.CENTER_VERTICAL|Gravity.RIGHT);title.setMaxLines(2);title.setEllipsize(null);
                row.addView(balance,new LinearLayout.LayoutParams(0,dp(40),1.05f));row.addView(title,new LinearLayout.LayoutParams(0,dp(40),1.65f));
                list.addView(row,new LinearLayout.LayoutParams(-1,dp(46)));addSpaceTo(list,3);
            }c.close();
        };
        add.setOnClickListener(v->{String n=name.getText().toString().trim();if(n.isEmpty()){Toast.makeText(this,"اكتب اسم العميل",Toast.LENGTH_SHORT).show();return;}db.addCustomer(n,phone.getText().toString().trim());name.setText("");phone.setText("");refresh[0].run();});
        search.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){refresh[0].run();}public void afterTextChanged(android.text.Editable e){}});
        refresh[0].run();
    }

    void addSpaceTo(LinearLayout p,int h){Space s=new Space(this);p.addView(s,new LinearLayout.LayoutParams(1,h));}
    void account(long id,String name){
        base("حساب العميل");
        LinearLayout customerHeader=new LinearLayout(this);customerHeader.setOrientation(LinearLayout.HORIZONTAL);customerHeader.setGravity(Gravity.CENTER_VERTICAL);
        customerHeader.setPadding(dp(8),dp(3),dp(8),dp(3));customerHeader.setBackground(outlined(CARD,1,12));customerHeader.setOnClickListener(v->customerActions(id,name));
        TextView headerBalance=tv(balanceText(db.balance(id)),14);headerBalance.setTextColor(balanceColor(db.balance(id)));headerBalance.setTypeface(Typeface.DEFAULT,Typeface.BOLD);headerBalance.setGravity(Gravity.CENTER);
        TextView headerName=tv(name,16);headerName.setTextColor(GREEN);headerName.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        customerHeader.addView(headerBalance,new LinearLayout.LayoutParams(0,dp(42),1.05f));customerHeader.addView(headerName,new LinearLayout.LayoutParams(0,dp(42),1.65f));
        content.addView(customerHeader,new LinearLayout.LayoutParams(-1,dp(50)));space(6);
        LinearLayout summary=card();TextView bal=tv(balanceText(db.balance(id)),18);bal.setTextColor(balanceColor(db.balance(id)));bal.setTypeface(Typeface.DEFAULT,Typeface.BOLD);bal.setGravity(Gravity.CENTER);summary.addView(bal,new LinearLayout.LayoutParams(-1,dp(38)));
        TextView hint=tv("الضغط على العملية لعرض بياناتها • الضغط المطول للخيارات",10);hint.setTextColor(MUTED);hint.setGravity(Gravity.CENTER);summary.addView(hint,new LinearLayout.LayoutParams(-1,dp(28)));addCard(summary,76);
        EditText amount=numberField("المبلغ"),details=field("التفاصيل");addField(amount);addField(details);
        LinearLayout acts=new LinearLayout(this);acts.setOrientation(LinearLayout.HORIZONTAL);Button debit=button("عليه"),credit=button("له / دفعة");debit.setTextColor(Color.RED);credit.setTextColor(GREEN);
        acts.addView(debit,new LinearLayout.LayoutParams(0,dp(40),1));acts.addView(credit,new LinearLayout.LayoutParams(0,dp(40),1));content.addView(acts);addSpace(6);
        Button saveAccountImage=button("🖼 حفظ صورة كشف الحساب");saveAccountImage.setTextColor(GREEN);content.addView(saveAccountImage,new LinearLayout.LayoutParams(-1,dp(40)));saveAccountImage.setOnClickListener(v->saveAccountStatementImage(id,name)); addSpace(3); Button sharePdf=button("📄 كشف الحساب PDF + واتساب");sharePdf.setTextColor(GREEN);content.addView(sharePdf,new LinearLayout.LayoutParams(-1,dp(42)));sharePdf.setOnClickListener(v->shareAccountPdfToWhatsApp(id,name));
        section("سجل العمليات");
        LinearLayout selectedActions=new LinearLayout(this);selectedActions.setOrientation(LinearLayout.HORIZONTAL);Button shareSelected=button("📤 مشاركة المحدد"),printSelected=button("🖨 طباعة المحدد");shareSelected.setTextColor(GREEN);printSelected.setTextColor(GREEN);
        selectedActions.addView(shareSelected,new LinearLayout.LayoutParams(0,dp(40),1));selectedActions.addView(printSelected,new LinearLayout.LayoutParams(0,dp(40),1));content.addView(selectedActions);addSpace(5);
        // سجل العمليات مصمم لعرض كامل البيانات داخل شاشة الجوال بدون تمرير أفقي.
        LinearLayout table=new LinearLayout(this);table.setOrientation(LinearLayout.VERTICAL);table.setPadding(0,0,0,dp(4));
        LinearLayout header=new LinearLayout(this);header.setOrientation(LinearLayout.HORIZONTAL);header.setGravity(Gravity.CENTER_VERTICAL);header.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);header.setBackground(outline(Color.rgb(241,247,242),8));
        String[] heads={"الرصيد","المبلغ","الفاتورة","التفاصيل","التاريخ والوقت"};float[] hw={1.0f,0.92f,0.72f,1.55f,1.05f};
        for(int i=0;i<heads.length;i++){TextView h=tv(heads[i],9);h.setTextColor(GREEN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setGravity(Gravity.CENTER);h.setMaxLines(2);header.addView(h,new LinearLayout.LayoutParams(0,dp(34),hw[i]));}
        table.addView(header,new LinearLayout.LayoutParams(-1,dp(36)));
        final ArrayList<Long> selected=new ArrayList<>();
        final Runnable[] refreshHolder=new Runnable[1]; Runnable refresh=()->{
            while(table.getChildCount()>1)table.removeViewAt(1);selected.clear();
            double runningAfter=db.balance(id);Cursor c=db.transactions(id);
            while(c.moveToNext()){
                long tid=c.getLong(0);String date=c.getString(1),d=c.getString(2);double a=c.getDouble(3);int type=c.getInt(4);String invNo=db.invoiceNoFromTransaction(d);
                boolean invoiceEntry=!invNo.isEmpty();
                double invoiceTotal=invoiceEntry?db.invoiceTotalByNo(invNo):0;
                double invoicePaid=invoiceEntry?db.invoicePaidByNo(invNo):0;
                boolean invoiceSettled=invoiceEntry && invoicePaid>=invoiceTotal;
                int operationColor=invoiceEntry?(invoiceSettled?BLUE:RED):(type==1?RED:BLUE);
                LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setGravity(Gravity.CENTER_VERTICAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);r.setPadding(dp(1),dp(1),dp(1),dp(1));r.setBackground(outlined(CARD,1,8));
                TextView rv=tv(balanceText(runningAfter),8);rv.setTextColor(balanceColor(runningAfter));rv.setGravity(Gravity.CENTER);rv.setMaxLines(2);
                TextView av=tv((type==1?"عليه ":"له / دفعة ")+fmt(a),8);av.setTextColor(operationColor);av.setTypeface(Typeface.DEFAULT,Typeface.BOLD);av.setGravity(Gravity.CENTER);av.setMaxLines(2);
                TextView iv=tv(invNo.isEmpty()?"—":invNo,8);iv.setGravity(Gravity.CENTER);iv.setMaxLines(2);
                TextView dv=tv(d==null||d.trim().isEmpty()?"عملية مالية":d,8);dv.setGravity(Gravity.CENTER);dv.setMaxLines(3);dv.setEllipsize(null);
                TextView dt=tv(date,7);dt.setTextColor(MUTED);dt.setGravity(Gravity.CENTER);dt.setMaxLines(2);
                View[] cells={rv,av,iv,dv,dt};
                for(int i=0;i<cells.length;i++)r.addView(cells[i],new LinearLayout.LayoutParams(0,dp(48),hw[i]));
                CheckBox check=new CheckBox(this);check.setText("");check.setGravity(Gravity.CENTER);check.setPadding(0,0,0,0);
                r.addView(check,new LinearLayout.LayoutParams(dp(24),dp(48)));
                r.setOnClickListener(v->showOperationDetails(name,tid,d,a,type));r.setOnLongClickListener(v->{operationActions(id,name,tid,d,a,type);return true;});
                check.setOnCheckedChangeListener((b,is)->{if(is){if(!selected.contains(tid))selected.add(tid);}else selected.remove(tid);});
                table.addView(r,new LinearLayout.LayoutParams(-1,dp(50)));addSpaceTo(table,2);runningAfter-=(type==1?a:-a);
            }c.close();bal.setText(balanceText(db.balance(id)));bal.setTextColor(balanceColor(db.balance(id)));headerBalance.setText(balanceText(db.balance(id)));headerBalance.setTextColor(balanceColor(db.balance(id)));
        };
        content.addView(table,new LinearLayout.LayoutParams(-1,-2));
        View.OnClickListener addOp=v->{try{double a=Double.parseDouble(amount.getText().toString().trim());if(a<=0)throw new Exception();db.addTransaction(id,a,details.getText().toString().trim(),v==debit?1:0,db.now());amount.setText("");details.setText("");refresh.run();showPostSaveOperation("تم حفظ العملية","العميل: "+name+"\nالمبلغ: "+(v==debit?"عليه ":"له ")+fmt(a)+" ريال\nالرصيد الحالي: "+balanceText(db.balance(id)),()->shareOperation(name,details.getText().toString().trim(),a,v==debit?1:0,""),()->{});}catch(Exception e){Toast.makeText(this,"أدخل المبلغ بشكل صحيح",Toast.LENGTH_SHORT).show();}};
        debit.setOnClickListener(addOp);credit.setOnClickListener(addOp);
        shareSelected.setOnClickListener(v->{if(selected.isEmpty())Toast.makeText(this,"حدد عملية واحدة أو أكثر أولاً",Toast.LENGTH_SHORT).show();else shareSelectedTransactions(id,name,new ArrayList<>(selected));});
        printSelected.setOnClickListener(v->{if(selected.isEmpty())Toast.makeText(this,"حدد عملية واحدة أو أكثر أولاً",Toast.LENGTH_SHORT).show();else printSelectedTransactions(id,name,new ArrayList<>(selected));});
        refresh.run();
    }

    void customerActions(long id,String name){
        String[] choices={"✏ تعديل بيانات العميل","📄 كشف الحساب PDF + واتساب","📞 اتصال بالعميل","🗑 حذف حساب العميل"};
        new AlertDialog.Builder(this).setTitle("حساب: "+name).setItems(choices,(d,w)->{
            if(w==0)editCustomer(id,name);
            else if(w==1)shareAccountPdfToWhatsApp(id,name);
            else if(w==2){
                String p=db.phoneByName(name).replaceAll("[^0-9+]","");
                if(p.isEmpty()){Toast.makeText(this,"لا يوجد رقم هاتف للعميل",Toast.LENGTH_SHORT).show();return;}
                try{startActivity(new Intent(Intent.ACTION_DIAL,Uri.parse("tel:"+p)));}catch(Exception ignored){}
            }else new AlertDialog.Builder(this).setTitle("حذف حساب العميل؟").setMessage("سيتم حذف الحساب وجميع عملياته وفواتيره المرتبطة به.").setPositiveButton("حذف",(x,y)->{db.deleteCustomer(id);customers();}).setNegativeButton("إلغاء",null).show();
        }).setNegativeButton("إغلاق",null).show();
    }

    void editCustomer(long id,String oldName){
        EditText name=field("اسم العميل"); name.setText(oldName);
        EditText phone=phoneField("رقم الهاتف"); phone.setText(db.phoneByName(oldName));
        LinearLayout box=new LinearLayout(this); box.setOrientation(LinearLayout.VERTICAL); box.setPadding(dp(8),dp(4),dp(8),dp(4));
        box.addView(name,new LinearLayout.LayoutParams(-1,dp(40))); spaceInside(box,5); box.addView(phone,new LinearLayout.LayoutParams(-1,dp(40)));
        new AlertDialog.Builder(this).setTitle("تعديل بيانات العميل").setView(box)
            .setNegativeButton("إلغاء",null)
            .setPositiveButton("حفظ",(d,w)->{
                String n=name.getText().toString().trim(), p=phone.getText().toString().trim();
                if(n.isEmpty()){Toast.makeText(this,"اسم العميل مطلوب",Toast.LENGTH_SHORT).show();return;}
                db.updateCustomer(id,oldName,n,p); customers();
                Toast.makeText(this,"تم تعديل بيانات العميل",Toast.LENGTH_SHORT).show();
            }).show();
    }

    void operationActions(long customerId,String customerName,long tid,String details,double amount,int type){
        String invNo=db.invoiceNoFromTransaction(details);ArrayList<String> choices=new ArrayList<>();
        if(!invNo.isEmpty())choices.add("🧾 تعديل الفاتورة");
        choices.add("✏ تعديل العملية");choices.add("📤 مشاركة العملية واتساب");choices.add("🖼 مشاركة صورة");choices.add("🖨 طباعة الحالية");choices.add("🗑 حذف العملية");
        String[] a=choices.toArray(new String[0]);
        new AlertDialog.Builder(this).setTitle("خيارات العملية").setItems(a,(d,w)->{
            int i=0;
            if(!invNo.isEmpty()&&w==i++){long iid=db.invoiceIdByNo(invNo);if(iid>0)invoice(true,iid);return;}
            if(w==i++){editTransaction(customerId,customerName,tid,amount,details,type);return;}
            if(w==i++){shareOperation(customerName,details,amount,type,invNo);return;}
            if(w==i++){shareOperationImage(customerName,details,amount,type,invNo);return;}
            if(w==i++){printOperation(customerName,details,amount,type,invNo);return;}
            new AlertDialog.Builder(this).setTitle("حذف العملية؟").setPositiveButton("حذف",(x,y)->{db.deleteTransaction(tid);account(customerId,customerName);}).setNegativeButton("إلغاء",null).show();
        }).setNegativeButton("إغلاق",null).show();
    }

    void editTransaction(long id,String name,long tid,double oldAmount,String oldDetails,int oldType){
        EditText amount=numberField("المبلغ");amount.setText(fmt(oldAmount));EditText details=field("التفاصيل");details.setText(oldDetails==null?"":oldDetails);
        LinearLayout box=new LinearLayout(this);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(8),dp(4),dp(8),dp(4));box.addView(amount);spaceInside(box,4);box.addView(details);        new AlertDialog.Builder(this).setTitle("تعديل العملية").setView(box).setPositiveButton("حفظ",(d,w)->{
            try{double a=Double.parseDouble(amount.getText().toString().trim());if(a<=0)throw new Exception();db.updateTransaction(tid,a,details.getText().toString().trim(),oldType,db.now());account(id,name);}
            catch(Exception e){Toast.makeText(this,"بيانات العملية غير صحيحة",Toast.LENGTH_SHORT).show();}
        }).setNegativeButton("إلغاء",null).show();
    }

    String compactOperationText(String customer,String details,double amount,int type,String invNo){
        StringBuilder t=new StringBuilder();
        t.append("بقالة العزي\nعملية حسابية\n");
        t.append("العميل: ").append(customer==null||customer.trim().isEmpty()?"نقدي":customer.trim()).append("\n");
        if(invNo!=null&&!invNo.isEmpty())t.append("الفاتورة: ").append(invNo).append("\n");
        if(details!=null&&!details.trim().isEmpty()&&!details.startsWith("فاتورة مبيعات رقم "))t.append("البيان: ").append(details.trim()).append("\n");
        t.append("المبلغ: ").append(type==1?"عليه ":"له ").append(fmt(amount)).append(" ريال\n");
        t.append("الرصيد الحالي: ").append(balanceText(db.balanceByName(customer))).append("\n");
        t.append("التاريخ: ").append(db.now());
        return t.toString();
    }
    void shareOperation(String customer,String details,double amount,int type,String invNo){
        shareWhatsAppToCustomer(db.phoneByName(customer),compactOperationText(customer,details,amount,type,invNo),null);
    }

    void shareOperationImage(String customer,String details,double amount,int type,String invNo){
        try{
            String text=compactOperationText(customer,details,amount,type,invNo);
            Uri uri=saveReceiptBitmap(operationBitmap(text),invNo==null||invNo.isEmpty()?String.valueOf(System.currentTimeMillis()):"عملية_"+invNo);
            shareWhatsAppToCustomer(db.phoneByName(customer),text,uri);
        }catch(Exception e){shareOperation(customer,details,amount,type,invNo);}
    }

    Bitmap operationBitmap(String text){
        final int width=384,margin=20;
        Bitmap b=Bitmap.createBitmap(width,360,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(b);c.drawColor(Color.WHITE);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);p.setTextAlign(Paint.Align.CENTER);
        p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(19);p.setColor(GREEN);
        c.drawText("بقالة العزي",width/2,42,p);
        p.setTypeface(Typeface.create("sans",Typeface.BOLD));p.setTextSize(14);p.setColor(Color.BLACK);
        c.drawText("بيان عملية",width/2,66,p);
        p.setTypeface(Typeface.create("sans",Typeface.NORMAL));p.setTextSize(11);p.setTextAlign(Paint.Align.RIGHT);
        int y=94;
        for(String line:text.split("\\n",-1)){
            if(line.startsWith("بقالة العزي")||line.equals("عملية حسابية"))continue;
            if(y>325)break;
            c.drawText(line,width-margin,y,p);y+=23;
        }
        p.setColor(Color.rgb(24,112,61));c.drawLine(margin,y,width-margin,y,p);
        return b;
    }

    void shareSelectedTransactions(long customerId,String name,ArrayList<Long> ids){
        StringBuilder text=new StringBuilder("بقالة العزي\nكشف عمليات\nالعميل: ").append(name).append("\n");
        double debit=0,credit=0;
        for(Long tid:ids){Cursor c=db.transactionById(tid);if(c.moveToFirst()){String d=c.getString(3);double a=c.getDouble(4);int t=c.getInt(5);
            text.append(c.getString(2)).append(" • ").append(t==1?"عليه ":"له ").append(fmt(a)).append(" ريال");
            if(d!=null&&!d.trim().isEmpty())text.append(" • ").append(d.trim());
            text.append("\n");if(t==1)debit+=a;else credit+=a;}c.close();}
        text.append("\nعليه: ").append(fmt(debit)).append(" ريال • له: ").append(fmt(credit)).append(" ريال");
        text.append("\nالرصيد الحالي: ").append(balanceText(db.balance(customerId)));
        shareWhatsAppToCustomer(db.phoneByName(name),text.toString(),null);
    }

    void printSelectedTransactions(long customerId,String name,ArrayList<Long> ids){
        StringBuilder text=new StringBuilder("بقالة العزي\\nكشف عمليات: ").append(name).append("\\n");
        double debit=0,credit=0;
        for(Long tid:ids){
            Cursor c=db.transactionById(tid);
            if(c.moveToFirst()){
                String d=c.getString(3);double a=c.getDouble(4);int t=c.getInt(5);
                text.append(c.getString(2)).append(" | ").append(d==null?"":d).append(" | ").append(t==1?"عليه: ":"له: ").append(fmt(a)).append(" ريال\\n");
                if(t==1)debit+=a;else credit+=a;
            }c.close();
        }
        text.append("------------------------------\\nإجمالي المحدد عليه: ").append(fmt(debit)).append(" ريال\\n");
        text.append("إجمالي المحدد له: ").append(fmt(credit)).append(" ريال\\n");
        text.append("الرصيد الحالي: ").append(balanceText(db.balance(customerId)));
        previewTextForPrint(text.toString(),name);
    }

    void printOperation(String customer,String details,double amount,int type,String invNo){
        try{
            ArrayList<Line> ls=new ArrayList<>();
            if(!invNo.isEmpty()){long iid=db.invoiceIdByNo(invNo);if(iid>0){Cursor c=db.invoiceLines(iid);while(c.moveToNext())ls.add(new Line(c.getString(1),c.getDouble(2),c.getDouble(3)));c.close();}}
            String text=!ls.isEmpty()?receiptTextFromLines(invNo,customer,ls,totalOf(ls),db.customer(customer)):
                "بقالة العزي\\nعملية مالية\\nالعميل: "+customer+"\\n"+details+"\\n"+(type==1?"عليه: ":"له: ")+fmt(amount)+" ريال\\n"+balanceText(db.balanceByName(customer));
            previewTextForPrint(text,customer);
        }catch(Exception e){Toast.makeText(this,"تعذر تجهيز العملية للطباعة",Toast.LENGTH_LONG).show();}
    }

    void previewTextForPrint(String text,String customer){
        TextView v=tv(text,11);v.setGravity(Gravity.CENTER);v.setTypeface(Typeface.MONOSPACE);
        new AlertDialog.Builder(this).setTitle("معاينة العملية 58mm").setView(v).setPositiveButton("طباعة",(d,w)->printTextBluetooth(text)).setNegativeButton("إغلاق",null).show();
    }
    String statement(long id,String name){
        StringBuilder s=new StringBuilder();
        s.append("بقالة العزي\nكشف حساب\n");
        s.append("العميل: ").append(name).append("\n");
        s.append("التاريخ: ").append(db.now()).append("\n\n");
        double running=db.balance(id),debit=0,credit=0;Cursor c=db.transactions(id);
        while(c.moveToNext()){
            String date=c.getString(1),details=c.getString(2);double amount=c.getDouble(3);int type=c.getInt(4);
            if(type==1)debit+=amount;else credit+=amount;
            String inv=db.invoiceNoFromTransaction(details);
            s.append(date).append("\n");
            s.append(type==1?"عليه: ":"له: ").append(fmt(amount)).append(" ريال");
            if(!inv.isEmpty())s.append(" • فاتورة ").append(inv);
            s.append("\n");
            if(details!=null&&!details.trim().isEmpty())s.append(details.trim()).append("\n");
            s.append("الرصيد بعد العملية: ").append(balanceText(running)).append("\n\n");
            running-=(type==1?amount:-amount);
        }c.close();
        s.append("ــــــــــــــــــــ\n");
        s.append("إجمالي عليه: ").append(fmt(debit)).append(" ريال\n");
        s.append("إجمالي له: ").append(fmt(credit)).append(" ريال\n");
        s.append("الرصيد الحالي: ").append(balanceText(db.balance(id)));
        return s.toString();
    }

    void inventory(){
        base("المخزون");section("إضافة / تعديل صنف");
        EditText name=field("اسم الصنف");EditText qty=numberField("الكمية");EditText min=numberField("الحد الأدنى");
        addField(name);addField(qty);addField(min);
        LinearLayout stockForm=new LinearLayout(this); stockForm.setOrientation(LinearLayout.HORIZONTAL); stockForm.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button add=button("＋ حفظ الصنف");add.setTextColor(Color.WHITE);add.setBackgroundColor(GREEN);
        Button clearFormBtn=button("مسح");clearFormBtn.setTextColor(MUTED);
        stockForm.addView(add,new LinearLayout.LayoutParams(0,dp(40),1.7f));
        stockForm.addView(clearFormBtn,new LinearLayout.LayoutParams(0,dp(40),.65f));
        content.addView(stockForm,new LinearLayout.LayoutParams(-1,dp(42)));addSpace(8);
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);

        final long[] editingId={-1};

        Runnable clearForm=()->{
            editingId[0]=-1;
            name.setText("");qty.setText("");min.setText("");
            add.setText("＋ حفظ الصنف");
        };

        clearFormBtn.setOnClickListener(v->clearForm.run());

        final Runnable[] refresh={null};
        refresh[0]=()->{
            list.removeAllViews();
            Cursor c=db.items();
            while(c.moveToNext()){
                long id=c.getLong(0);
                String itemName=c.getString(1);
                double q=c.getDouble(2),m=c.getDouble(3);

                LinearLayout row=new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(8),dp(5),dp(8),dp(5));
                row.setBackground(outlined(CARD,1,10));

                TextView info=tv(itemName+"\nالكمية: "+fmt(q)+"   •   الحد الأدنى: "+fmt(m)+(q<=m?"   ⚠ منخفض":""),13);
                info.setTextColor(q<=m?Color.rgb(170,75,35):TEXT);
                row.addView(info,new LinearLayout.LayoutParams(-1,dp(40)));

                LinearLayout actions=new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setGravity(Gravity.CENTER);

                Button editBtn=button("✎ تعديل");
                editBtn.setTextColor(GREEN);
                Button deleteBtn=button("حذف");
                deleteBtn.setTextColor(Color.rgb(170,55,55));

                editBtn.setOnClickListener(v->{
                    editingId[0]=id;
                    name.setText(itemName);qty.setText(fmt(q));min.setText(fmt(m));
                    add.setText("✓ حفظ التعديل");
                    name.requestFocus();
                    Toast.makeText(this,"تم تحميل الصنف للتعديل",Toast.LENGTH_SHORT).show();
                });

                deleteBtn.setOnClickListener(v->new AlertDialog.Builder(this)
                    .setTitle("حذف الصنف")
                    .setMessage("هل تريد حذف «"+itemName+"» نهائياً؟")
                    .setNegativeButton("إلغاء",null)
                    .setPositiveButton("حذف",(d,w)->{
                        db.deleteItem(id);
                        if(editingId[0]==id)clearForm.run();
                        refresh[0].run();
                        Toast.makeText(this,"تم حذف الصنف",Toast.LENGTH_SHORT).show();
                    }).show());

                actions.addView(editBtn,new LinearLayout.LayoutParams(0,dp(32),1));
                actions.addView(deleteBtn,new LinearLayout.LayoutParams(0,dp(34),1));
                row.addView(actions);
                list.addView(row,new LinearLayout.LayoutParams(-1,dp(82)));
                addSpaceTo(list,5);
            }
            c.close();
        };

        add.setOnClickListener(v->{
            try{
                String n=name.getText().toString().trim();
                double q=Double.parseDouble(qty.getText().toString().trim());
                double m=Double.parseDouble(min.getText().toString().trim());
                if(n.isEmpty()||q<0||m<0)throw new Exception();

                if(editingId[0]>0){
                    db.updateItem(editingId[0],n,q,m);
                    Toast.makeText(this,"تم تعديل الصنف وحفظه",Toast.LENGTH_SHORT).show();
                }else{
                    boolean existed=db.itemExists(n);
                    db.addItem(n,q,m);
                    Toast.makeText(this,existed?"الصنف موجود؛ تم تحديث بياناته":"تم حفظ الصنف",Toast.LENGTH_SHORT).show();
                }
                clearForm.run();
                refresh[0].run();
            }catch(Exception e){
                Toast.makeText(this,"أدخل بيانات الصنف بشكل صحيح",Toast.LENGTH_SHORT).show();
            }
        });
        refresh[0].run();
    }
    static class NoteItem { String name; double qty; int side; NoteItem(String n,double q,int s){name=n;qty=q;side=s;} }
    void notes(){ base("الملاحظات");
        LinearLayout top=new LinearLayout(this);top.setOrientation(LinearLayout.HORIZONTAL);top.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        Button clear=action("🧹 تفريغ الصفحة",Color.rgb(235,130,35));clear.setOnClickListener(v->clearNotesPage());
        Button fresh=action("＋ صفحة ملاحظة جديدة",Color.rgb(35,155,190));fresh.setOnClickListener(v->newNotesPage());
        Button history=action("📚 سجل الصفحات",Color.rgb(125,70,170));history.setOnClickListener(v->showNotesHistory());
        Button print=action("🖨 الطباعة الذكية",GREEN);print.setOnClickListener(v->printCurrentNotes());
        top.addView(clear,new LinearLayout.LayoutParams(0,dp(48),1));top.addView(fresh,new LinearLayout.LayoutParams(0,dp(48),1));top.addView(history,new LinearLayout.LayoutParams(0,dp(48),1));top.addView(print,new LinearLayout.LayoutParams(0,dp(48),1));content.addView(top,new LinearLayout.LayoutParams(-1,dp(50)));addSpace(5);
        LinearLayout controls=card();LinearLayout cr=new LinearLayout(this);cr.setGravity(Gravity.CENTER);Button minus=button("−");TextView fs=tv("حجم الخط "+noteFontSize,11);fs.setGravity(Gravity.CENTER);Button plus=button("+");minus.setOnClickListener(v->{noteFontSize=Math.max(10,noteFontSize-1);notes();});plus.setOnClickListener(v->{noteFontSize=Math.min(24,noteFontSize+1);notes();});cr.addView(minus,new LinearLayout.LayoutParams(dp(38),dp(34)));cr.addView(fs,new LinearLayout.LayoutParams(dp(100),dp(34)));cr.addView(plus,new LinearLayout.LayoutParams(dp(38),dp(34)));Switch sw=new Switch(this);sw.setText("وضع التمرير: "+(noteScrollMode?"مفعل":"متوقف"));sw.setChecked(noteScrollMode);sw.setOnCheckedChangeListener((b,x)->{noteScrollMode=x;b.setText("وضع التمرير: "+(x?"مفعل":"متوقف"));});cr.addView(sw,new LinearLayout.LayoutParams(-2,dp(34)));controls.addView(cr);content.addView(controls,new LinearLayout.LayoutParams(-1,dp(44)));addSpace(5);
        if(currentNotePageId<1)currentNotePageId=db.createNotePage("ملاحظة جديدة",db.now());final long pid=currentNotePageId;ArrayList<NoteItem> left=new ArrayList<>(),right=new ArrayList<>();db.loadNoteItems(pid,left,right);
        LinearLayout form=card();LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.HORIZONTAL);fields.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);EditText qty=numberField("العدد / الرقم");qty.setText("1");EditText name=field("اكتب اسم الصنف...");fields.addView(qty,new LinearLayout.LayoutParams(0,dp(40),.8f));LinearLayout.LayoutParams np=new LinearLayout.LayoutParams(0,dp(40),2.1f);np.setMargins(dp(4),0,dp(4),0);fields.addView(name,np);form.addView(fields);
        LinearLayout adds=new LinearLayout(this);adds.setOrientation(LinearLayout.HORIZONTAL);Button al=button("＋ للشق الأيسر");al.setTextColor(Color.WHITE);al.setBackground(rounded(GREEN,dp(10)));al.setOnClickListener(v->addNoteItem(pid,name,qty,1));Button ar=button("＋ للشق الأيمن");ar.setTextColor(Color.WHITE);ar.setBackground(rounded(BLUE,dp(10)));ar.setOnClickListener(v->addNoteItem(pid,name,qty,2));adds.addView(al,new LinearLayout.LayoutParams(0,dp(38),1));LinearLayout.LayoutParams arp=new LinearLayout.LayoutParams(0,dp(38),1);arp.setMargins(dp(4),0,0,0);adds.addView(ar,arp);form.addView(adds);content.addView(form,new LinearLayout.LayoutParams(-1,dp(88)));addSpace(5);
        LinearLayout split=new LinearLayout(this);split.setOrientation(LinearLayout.HORIZONTAL);split.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);split.addView(noteColumn("الشق الأيسر",left,1,pid),new LinearLayout.LayoutParams(0,-2,1));LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(0,-2,1);rp.setMargins(dp(4),0,0,0);split.addView(noteColumn("الشق الأيمن",right,2,pid),rp);content.addView(split,new LinearLayout.LayoutParams(-1,-2));
    }
    LinearLayout noteColumn(String title,ArrayList<NoteItem> items,int side,long pid){LinearLayout col=new LinearLayout(this);col.setOrientation(LinearLayout.VERTICAL);col.setPadding(dp(3),dp(3),dp(3),dp(5));col.setBackground(outlined(Color.rgb(252,253,252),1,12));TextView h=tv(title,11);h.setTextColor(side==1?GREEN:BLUE);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setGravity(Gravity.CENTER);col.addView(h,new LinearLayout.LayoutParams(-1,dp(30)));if(items.isEmpty()){TextView e=tv("لا توجد عناصر",9);e.setTextColor(MUTED);e.setGravity(Gravity.CENTER);col.addView(e,new LinearLayout.LayoutParams(-1,dp(40)));return col;}for(NoteItem it:items){LinearLayout row=new LinearLayout(this);row.setOrientation(LinearLayout.HORIZONTAL);row.setGravity(Gravity.CENTER_VERTICAL);row.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);row.setBackground(outlined(CARD,1,9));Button del=button("🗑");del.setTextColor(RED);del.setOnClickListener(v->{db.deleteNoteItem(pid,it.name,it.qty,it.side);notes();});TextView nm=tv(it.name,noteFontSize);nm.setTextColor(Color.rgb(20,65,120));nm.setMaxLines(2);TextView q=tv(fmt(it.qty),noteFontSize);q.setGravity(Gravity.CENTER);q.setTypeface(Typeface.DEFAULT,Typeface.BOLD);row.addView(del,new LinearLayout.LayoutParams(dp(38),dp(44)));row.addView(nm,new LinearLayout.LayoutParams(0,dp(44),1));row.addView(q,new LinearLayout.LayoutParams(dp(45),dp(44)));col.addView(row,new LinearLayout.LayoutParams(-1,dp(46)));addSpaceTo(col,2);}return col;}
    void addNoteItem(long pid,EditText name,EditText qty,int side){String n=name.getText().toString().trim();double q=0; try { q=Double.parseDouble(qty.getText().toString().trim().replace(",", ".")); } catch(Exception ignored) {}if(n.isEmpty()){Toast.makeText(this,"اكتب اسم الصنف أولاً",Toast.LENGTH_SHORT).show();return;}if(q<=0){Toast.makeText(this,"العدد يجب أن يكون أكبر من صفر",Toast.LENGTH_SHORT).show();return;}db.addNoteItem(pid,n,q,side);name.setText("");qty.setText("1");notes();}
    void clearNotesPage(){if(currentNotePageId<1)return;new AlertDialog.Builder(this).setTitle("تفريغ الصفحة").setMessage("سيتم حذف عناصر الصفحة الحالية فقط. هل تريد المتابعة؟").setNegativeButton("إلغاء",null).setPositiveButton("تفريغ",(d,w)->{db.clearNoteItems(currentNotePageId);notes();}).show();}
    void newNotesPage(){if(currentNotePageId>0)db.touchNotePage(currentNotePageId);currentNotePageId=db.createNotePage("ملاحظة جديدة",db.now());notes();}
    void showNotesHistory(){base("سجل الصفحات");section("الصفحات المحفوظة");Cursor c=db.notePages();while(c.moveToNext()){long id=c.getLong(0);String title=c.getString(1),date=c.getString(2);int n=c.getInt(3);LinearLayout row=card();TextView t=tv("📝 "+title+"\n"+date+" • "+n+" عنصر",12);t.setMaxLines(2);row.addView(t,new LinearLayout.LayoutParams(-1,dp(52)));row.setOnClickListener(v->{currentNotePageId=id;notes();});content.addView(row,new LinearLayout.LayoutParams(-1,dp(62)));addSpace(3);}c.close();}
    String notesReceiptText(){StringBuilder s=new StringBuilder("بقالة العزي\nالملاحظات الذكية\n");ArrayList<NoteItem> l=new ArrayList<>(),r=new ArrayList<>();db.loadNoteItems(currentNotePageId,l,r);s.append("------------------------------\nالشق الأيسر\n");for(NoteItem x:l)s.append(x.name).append(" × ").append(fmt(x.qty)).append("\n");s.append("------------------------------\nالشق الأيمن\n");for(NoteItem x:r)s.append(x.name).append(" × ").append(fmt(x.qty)).append("\n");return s.toString();}
    void printCurrentNotes(){if(currentNotePageId>0)printTextBluetooth(notesReceiptText());else Toast.makeText(this,"لا توجد صفحة ملاحظات للطباعة",Toast.LENGTH_SHORT).show();}
    void purchaseInvoices(){
        base("فواتير الشراء");
        section("فاتورة شراء جديدة");
        LinearLayout intro=card();
        TextView it=tv("إدخال فاتورة شراء",16);it.setTextColor(GOLD);it.setTypeface(Typeface.DEFAULT,Typeface.BOLD);it.setGravity(Gravity.CENTER);
        intro.addView(it,new LinearLayout.LayoutParams(-1,dp(30)));
        Button open=action("＋ فاتورة شراء جديدة",GOLD);open.setTextSize(13);open.setOnClickListener(v->newPurchaseInvoice());
        intro.addView(open,new LinearLayout.LayoutParams(-1,dp(42)));content.addView(intro,new LinearLayout.LayoutParams(-1,dp(82)));addSpace(7);
        section("سجل فواتير الشراء");
        LinearLayout list=new LinearLayout(this);list.setOrientation(LinearLayout.VERTICAL);content.addView(list);
        Cursor c=db.getReadableDatabase().rawQuery("SELECT id,no,supplier,total,date FROM purchase_invoices ORDER BY datetime(date) DESC,id DESC",null);
        while(c.moveToNext()){
            long id=c.getLong(0);String no=c.getString(1),supplier=c.getString(2),date=c.getString(4);double total=c.getDouble(3);
            LinearLayout row=card();row.setPadding(dp(7),dp(4),dp(7),dp(4));
            TextView info=tv("فاتورة شراء رقم "+no+"  •  "+(supplier==null||supplier.isEmpty()?"بدون مورد":supplier)+"\nالإجمالي: "+fmt(total)+" ريال  •  "+date,12);info.setMaxLines(2);info.setEllipsize(null);
            row.addView(info,new LinearLayout.LayoutParams(-1,dp(46)));
            row.setOnClickListener(v->showPurchaseInvoiceDialog(id,no,supplier,total,date));
            list.addView(row,new LinearLayout.LayoutParams(-1,dp(60)));addSpaceTo(list,3);
        }
        c.close();
    }

    void newPurchaseInvoice(){
        base("فاتورة شراء جديدة");
        section("بيانات فاتورة الشراء");
        LinearLayout meta=new LinearLayout(this);meta.setOrientation(LinearLayout.HORIZONTAL);meta.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        AutoCompleteTextView supplier=new AutoCompleteTextView(this);
        supplier.setHint("اسم المورد");supplier.setTextSize(13);supplier.setSingleLine(true);supplier.setTextColor(TEXT);supplier.setHintTextColor(MUTED);supplier.setPadding(dp(8),dp(3),dp(8),dp(3));supplier.setBackground(outline(CARD,10));supplier.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);supplier.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);supplier.setTextDirection(View.TEXT_DIRECTION_RTL);supplier.setThreshold(1);supplier.setSelectAllOnFocus(true);supplier.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,db.supplierNames()));
        EditText invoiceNo=field("رقم فاتورة الشراء");invoiceNo.setText(String.valueOf(db.nextPurchaseNo()));invoiceNo.setTextSize(13);
        meta.addView(supplier,new LinearLayout.LayoutParams(0,dp(40),1.35f));meta.addView(invoiceNo,new LinearLayout.LayoutParams(0,dp(40),1f));content.addView(meta,new LinearLayout.LayoutParams(-1,dp(42)));addSpace(6);

        section("إدخال الصنف");
        LinearLayout entry=card();entry.setPadding(dp(4),dp(6),dp(4),dp(7));
        LinearLayout fields=new LinearLayout(this);fields.setOrientation(LinearLayout.HORIZONTAL);fields.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        EditText total=numberField("القيمة الإجمالية");EditText qty=numberField("الكمية");qty.setText("1");
        AutoCompleteTextView item=new AutoCompleteTextView(this);
        item.setHint("اسم الصنف");item.setTextSize(13);item.setSingleLine(true);item.setTextColor(TEXT);item.setHintTextColor(MUTED);item.setPadding(dp(6),dp(2),dp(6),dp(2));item.setBackground(outline(CARD,10));item.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);item.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);item.setTextDirection(View.TEXT_DIRECTION_RTL);item.setSelectAllOnFocus(true);item.setThreshold(1);item.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_dropdown_item_1line,db.itemNames()));
        EditText unit=numberField("سعر الوحدة");unit.setEnabled(false);unit.setFocusable(false);unit.setClickable(false);unit.setLongClickable(false);unit.setTextColor(MUTED);unit.setBackground(outline(Color.rgb(242,244,242),10));
        EditText sale=numberField("سعر البيع");
        fields.addView(total,new LinearLayout.LayoutParams(0,dp(38),1.0f));fields.addView(qty,new LinearLayout.LayoutParams(0,dp(38),.72f));fields.addView(item,new LinearLayout.LayoutParams(0,dp(38),1.25f));fields.addView(unit,new LinearLayout.LayoutParams(0,dp(38),.9f));fields.addView(sale,new LinearLayout.LayoutParams(0,dp(38),.9f));entry.addView(fields);
        TextView note=tv("سعر الوحدة يُستنتج تلقائياً من القيمة الإجمالية ÷ الكمية ولا يمكن تعديله.",9);note.setTextColor(MUTED);note.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);entry.addView(note,new LinearLayout.LayoutParams(-1,dp(25)));
        Button add=action("＋ الإضافة إلى صندوق الفاتورة",GREEN);add.setTextSize(12);entry.addView(add,new LinearLayout.LayoutParams(-1,dp(40)));content.addView(entry,new LinearLayout.LayoutParams(-1,-2));addSpace(7);

        section("صندوق الفاتورة");
        LinearLayout box=card();box.setPadding(dp(3),dp(5),dp(3),dp(7));
        String[] heads={"القيمة الإجمالية","الكمية","اسم الصنف","سعر الوحدة","سعر البيع","حذف"};float[] w={1.0f,.72f,1.25f,.9f,.9f,.55f};
        LinearLayout head=new LinearLayout(this);head.setOrientation(LinearLayout.HORIZONTAL);head.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        for(int i=0;i<heads.length;i++){TextView h=tv(heads[i],8);h.setTextColor(GREEN);h.setTypeface(Typeface.DEFAULT,Typeface.BOLD);h.setGravity(Gravity.CENTER);h.setMaxLines(2);head.addView(h,new LinearLayout.LayoutParams(0,dp(34),w[i]));}
        box.addView(head,new LinearLayout.LayoutParams(-1,dp(36)));
        LinearLayout rows=new LinearLayout(this);rows.setOrientation(LinearLayout.VERTICAL);rows.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);box.addView(rows);
        TextView grand=tv("إجمالي فاتورة الشراء: 0 ريال",17);grand.setTextColor(GREEN);grand.setTypeface(Typeface.DEFAULT,Typeface.BOLD);grand.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);grand.setBackground(bg(Color.rgb(255,249,226),10));box.addView(grand,new LinearLayout.LayoutParams(-1,dp(46)));
        content.addView(box,new LinearLayout.LayoutParams(-1,-2));addSpace(6);

        ArrayList<PurchaseLine> lines=new ArrayList<>();
        final Runnable[] redraw={null};
        redraw[0]=()->{
            rows.removeAllViews();double sum=0;
            for(PurchaseLine l:lines){
                sum+=l.total;LinearLayout r=new LinearLayout(this);r.setOrientation(LinearLayout.HORIZONTAL);r.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);r.setGravity(Gravity.CENTER_VERTICAL);
                String[] vals={fmt(l.total),fmt(l.qty),l.name,fmt(l.cost),fmt(l.sale)};
                for(int i=0;i<5;i++){TextView v=tv(vals[i],8);v.setGravity(i==2?Gravity.RIGHT|Gravity.CENTER_VERTICAL:Gravity.CENTER);v.setMaxLines(2);v.setEllipsize(TextUtils.TruncateAt.END);v.setBackground(outline(Color.rgb(248,250,248),6));r.addView(v,new LinearLayout.LayoutParams(0,dp(34),w[i]));}
                Button del=button("حذف");del.setTextSize(9);del.setTextColor(Color.RED);del.setBackgroundColor(Color.TRANSPARENT);del.setOnClickListener(v->{lines.remove(l);redraw[0].run();});r.addView(del,new LinearLayout.LayoutParams(0,dp(34),w[5]));rows.addView(r,new LinearLayout.LayoutParams(-1,dp(36)));addSpaceTo(rows,2);
            }
            grand.setText("إجمالي فاتورة الشراء: "+fmt(sum)+" ريال");
        };
        Runnable calc=()->{try{double t=Double.parseDouble(total.getText().toString().trim());double q=Double.parseDouble(qty.getText().toString().trim());unit.setText(q>0?fmt(t/q):"");}catch(Exception e){unit.setText("");}};
        total.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){calc.run();}public void afterTextChanged(android.text.Editable e){}});
        qty.addTextChangedListener(new android.text.TextWatcher(){public void beforeTextChanged(CharSequence s,int st,int c,int a){}public void onTextChanged(CharSequence s,int st,int b,int c){calc.run();}public void afterTextChanged(android.text.Editable e){}});
        add.setOnClickListener(v->{try{double t=Double.parseDouble(total.getText().toString().trim());double q=Double.parseDouble(qty.getText().toString().trim());double s=Double.parseDouble(sale.getText().toString().trim());String n=item.getText().toString().trim();if(n.isEmpty()||q<=0||t<0||s<0)throw new Exception();lines.add(new PurchaseLine(n,q,t/q,s,t));redraw[0].run();total.setText("");qty.setText("1");item.setText("");sale.setText("");unit.setText("");total.requestFocus();}catch(Exception e){Toast.makeText(this,"أدخل القيمة الإجمالية والكمية واسم الصنف وسعر البيع بشكل صحيح",Toast.LENGTH_SHORT).show();}});

        Button clear=btn("مسح أصناف الفاتورة");clear.setTextColor(MUTED);content.addView(clear,new LinearLayout.LayoutParams(-1,dp(36)));clear.setOnClickListener(v->{lines.clear();redraw[0].run();});addSpace(4);
        Button save=action("💾 حفظ فاتورة الشراء",GREEN);save.setTextSize(12);content.addView(save,new LinearLayout.LayoutParams(-1,dp(42)));addSpace(5);
        save.setOnClickListener(v->{try{
            String sn=supplier.getText().toString().trim(),no=invoiceNo.getText().toString().trim();if(sn.isEmpty()||no.isEmpty()||lines.isEmpty())throw new Exception();double sum=0;for(PurchaseLine l:lines)sum+=l.total;
            db.supplier(sn,"");
            long pid=db.addPurchase(no,sn,sum,db.now());
            if(pid<=0)throw new Exception("تعذر حفظ الفاتورة");
            db.replacePurchaseLines(pid,lines);
            db.updateStockFromPurchase(lines);
            Toast.makeText(this,"تم حفظ فاتورة الشراء وتحديث المخزون",Toast.LENGTH_LONG).show();purchaseInvoices();
        }catch(Exception e){Toast.makeText(this,"تحقق من اسم المورد ورقم الفاتورة والأصناف",Toast.LENGTH_SHORT).show();}});
        redraw[0].run();calc.run();
    }

    static class PurchaseLine{String name;double qty,cost,sale,total;PurchaseLine(String n,double q,double c,double s,double t){name=n;qty=q;cost=c;sale=s;total=t;}}
    void reports(){
        base("التقارير");
        section("ملخص التقارير والحركة");

        try{
            LinearLayout summary=card();
            summary.setPadding(dp(7),dp(3),dp(7),dp(3));
            TextView s=tv("الفواتير: "+db.invoiceCount()+"   •   المبيعات: "+fmt(db.sales())+" ريال   •   العملاء: "+db.customerCount(),11);
            s.setGravity(Gravity.CENTER);
            summary.addView(s,new LinearLayout.LayoutParams(-1,dp(34)));
            addCard(summary,50);

            section("جميع حركات التطبيق — الأحدث أولاً");
            TextView hint=tv("اضغط على أي حركة لعرض كامل بياناتها • الفاتورة تظهر بحجم أكبر",10);
            hint.setTextColor(MUTED);hint.setGravity(Gravity.CENTER);
            content.addView(hint,new LinearLayout.LayoutParams(-1,dp(24)));addSpace(2);

            Cursor c=db.recentActivity();
            while(c.moveToNext()){
                int kind=c.getInt(0);
                String ref=c.getString(1);
                String title=c.getString(2);
                double amount=c.getDouble(3);
                String date=c.getString(4);
                long sortId=c.getLong(5);

                final int fk=kind;
                final String fr=ref==null?"":ref;
                final String ft=title==null?"":title;
                final double fa=amount;
                final String fd=date==null?"":date;
                final long fid=sortId;
                int operationType=c.getInt(6);
                int activityColor=(kind==3)?GOLD:((kind==2 && operationType==1)?RED:BLUE);

                LinearLayout row=card();
                row.setPadding(dp(7),dp(kind==1?5:3),dp(7),dp(kind==1?5:3));
                row.setElevation(dp(1));

                String label=kind==1?"🧾 فاتورة":(kind==3?"🛒 فاتورة شراء":"عملية");
                String shortTitle=ft;
                TextView main=tv(label+"  •  "+shortTitle,kind==1?13:11);
                main.setTextColor(kind==1?GREEN:activityColor);
                main.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                main.setMaxLines(2);main.setEllipsize(null);
                row.addView(main,new LinearLayout.LayoutParams(-1,dp(kind==1?34:28)));

                TextView meta=tv(fmt(fa)+" ريال  •  "+fd,10);
                meta.setTextColor(MUTED);meta.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL);
                meta.setMaxLines(1);meta.setEllipsize(null);
                row.addView(meta,new LinearLayout.LayoutParams(-1,dp(22)));

                row.setOnClickListener(v->{
                    if(fk==3){
                        showPurchaseInvoiceDialog(fid,fr,db.purchaseSupplier(fid),fa,fd);
                    }else showReportActivityDetails(fk,fr,ft,fa,fd,fid);
                });
                if(kind==1){
                    row.setBackground(outlined(Color.rgb(250,253,250),1,12));
                    content.addView(row,new LinearLayout.LayoutParams(-1,dp(66)));
                }else{
                    int tint=operationType==1?Color.rgb(255,244,242):Color.rgb(241,248,255);
                    row.setBackground(outlined(tint,1,9));
                    content.addView(row,new LinearLayout.LayoutParams(-1,dp(52)));
                }
                addSpace(2);
            }
            c.close();
        }catch(Exception e){
            TextView err=tv("تعذر تحميل سجل الحركات. يمكنك الاستمرار باستخدام بقية الشاشات.",11);
            err.setTextColor(Color.rgb(170,75,35));
            content.addView(err,new LinearLayout.LayoutParams(-1,dp(44)));
        }
    }

    // فواتير الشراء تُعرض في التقارير بنفس أسلوب فواتير البيع.
    void showReportActivityDetails(int kind,String ref,String title,double amount,String date,long sortId){
        try{
            LinearLayout box=new LinearLayout(this);
            box.setOrientation(LinearLayout.VERTICAL);
            box.setPadding(dp(8),dp(4),dp(8),dp(4));

            if(kind==1){
                long invoiceId=db.invoiceIdByNo(ref);
                if(invoiceId>0){
                    String customer=db.invoiceCustomer(invoiceId);
                    double total=db.invoiceTotal(invoiceId);
                    double paid=db.invoicePaid(invoiceId);
                    String invoiceDate=db.invoiceDate(invoiceId);
                    long cid=customer==null||customer.trim().isEmpty()?-1:db.customerIdByName(customer.trim());
                    long tid=db.transactionIdForInvoice(ref);
                    double balanceAfter=tid>0?db.balanceAfterTransaction(tid):(cid>0?db.balance(cid):0);

                    TextView head=tv("فاتورة رقم "+ref,16);
                    head.setTextColor(GREEN);head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                    head.setGravity(Gravity.CENTER);
                    box.addView(head,new LinearLayout.LayoutParams(-1,dp(34)));

                    box.addView(detailLine("العميل",customer==null||customer.isEmpty()?"نقدي":customer));
                    box.addView(detailLine("التاريخ والوقت",invoiceDate==null||invoiceDate.isEmpty()?date:invoiceDate));
                    box.addView(detailLine("المبلغ الإجمالي",fmt(total)+" ريال"));
                    box.addView(detailLine("المبلغ المدفوع",fmt(paid)+" ريال"));
                    box.addView(detailLine("المتبقي",fmt(Math.max(0,total-paid))+" ريال"));
                    box.addView(detailLine("الرصيد بعد العملية",balanceText(balanceAfter)));

                    sectionInside(box,"أصناف الفاتورة");
                    Cursor lines=db.invoiceLines(invoiceId);
                    int count=0;
                    while(lines.moveToNext()){
                        String n=lines.getString(1);
                        double q=lines.getDouble(2),t=lines.getDouble(3);
                        TextView lr=tv(n+"   × "+fmt(q)+"   = "+fmt(t)+" ريال",11);
                        lr.setBackground(outline(Color.rgb(248,250,248),7));
                        lr.setMaxLines(2);lr.setEllipsize(null);
                        box.addView(lr,new LinearLayout.LayoutParams(-1,dp(30)));
                        spaceInside(box,2);count++;
                    }
                    lines.close();
                    if(count==0) box.addView(detailLine("الأصناف","لا توجد تفاصيل محفوظة"));

                    new AlertDialog.Builder(this).setTitle("تفاصيل الفاتورة").setView(box)
                        .setPositiveButton("إغلاق",null).show();
                    return;
                }
            }

            long tid=kind==2?sortId:-1;
            if(tid>0){
                Cursor tc=db.transactionById(tid);
                if(tc.moveToFirst()){
                    long customerId=tc.getLong(1);
                    String tdate=tc.getString(2);
                    String details=tc.getString(3);
                    double ta=tc.getDouble(4);
                    int type=tc.getInt(5);

                    String customerName=db.customerNameById(customerId);
                    double after=db.balanceAfterTransaction(tid);

                    TextView head=tv("تفاصيل العملية",15);
                    head.setTextColor(GREEN);head.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                    head.setGravity(Gravity.CENTER);
                    box.addView(head,new LinearLayout.LayoutParams(-1,dp(32)));
                    box.addView(detailLine("من الحساب",type==1?"المحل / المبيعات":"العميل"));
                    box.addView(detailLine("إلى الحساب",type==1?(customerName==null?"العميل":customerName):"المحل / الدفعات"));
                    box.addView(detailLine("العميل",customerName==null?"":customerName));
                    box.addView(detailLine("نوع العملية",type==1?"عليه":"له / دفعة"));
                    box.addView(detailLine("التفاصيل",details==null||details.trim().isEmpty()?"عملية مالية":details));
                    box.addView(detailLine("المبلغ",fmt(ta)+" ريال"));
                    box.addView(detailLine("التاريخ والوقت",tdate==null?"":tdate));
                    box.addView(detailLine("الرصيد بعد العملية",balanceText(after)));

                    tc.close();
                    new AlertDialog.Builder(this).setTitle("بيانات العملية كاملة").setView(box)
                        .setPositiveButton("إغلاق",null).show();
                    return;
                }
                tc.close();
            }

            new AlertDialog.Builder(this).setTitle("بيانات الحركة")
                .setMessage(title+"\nالمبلغ: "+fmt(amount)+" ريال\nالتاريخ والوقت: "+date)
                .setPositiveButton("إغلاق",null).show();
        }catch(Exception e){
            new AlertDialog.Builder(this).setTitle("بيانات الحركة")
                .setMessage("تعذر عرض كل تفاصيل هذه الحركة.")
                .setPositiveButton("إغلاق",null).show();
        }
    }

    TextView detailLine(String label,String value){
        TextView v=tv(label+": "+(value==null?"":value),11);
        v.setBackground(outline(Color.rgb(248,250,248),7));
        v.setMaxLines(3);v.setEllipsize(null);
        v.setPadding(dp(6),dp(2),dp(6),dp(2));
        return v;
    }

    // ==========================================
    // ماسح الفواتير الذكي (CamScanner)
    // ==========================================
    File getInvoicesImagesDir(){
        File dir=new File(getExternalFilesDir(null),"invoices_images");
        if(!dir.exists()) dir.mkdirs();
        return dir;
    }

    void launchScanCamera(){
        if(Build.VERSION.SDK_INT>=23 && checkSelfPermission(android.Manifest.permission.CAMERA)!=PackageManager.PERMISSION_GRANTED){
            requestPermissions(new String[]{android.Manifest.permission.CAMERA},REQ_PERM_CAMERA);
            return;
        }
        try{
            File tempFile=new File(getInvoicesImagesDir(),"cam_temp_"+System.currentTimeMillis()+".jpg");
            cameraScanTempUri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",tempFile);
            Intent i=new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            i.putExtra(MediaStore.EXTRA_OUTPUT,cameraScanTempUri);
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION|Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
            startActivityForResult(i,REQ_CAMERA_SCAN);
        }catch(Exception e){
            Toast.makeText(this,"تعذر تشغيل الكاميرا: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    void launchScanGallery(){
        try{
            Intent i=new Intent(Intent.ACTION_GET_CONTENT);
            i.setType("image/*");
            startActivityForResult(Intent.createChooser(i,"اختر صورة الفاتورة"),REQ_GALLERY_SCAN);
        }catch(Exception e){
            Toast.makeText(this,"تعذر فتح المعرض: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    void handleScanCameraResult(Intent data){
        try{
            Bitmap bmp=null;
            if(cameraScanTempUri!=null){
                try(InputStream is=getContentResolver().openInputStream(cameraScanTempUri)){
                    bmp=BitmapFactory.decodeStream(is);
                }
            }
            if(bmp==null&&data!=null&&data.getExtras()!=null){
                bmp=(Bitmap)data.getExtras().get("data");
            }
            if(bmp!=null){
                onImageCapturedForScan(bmp);
            }else{
                Toast.makeText(this,"لم يتم التقاط الصورة بنجاح",Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(this,"خطأ أثناء قراءة الصورة: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    void handleScanGalleryResult(Uri uri){
        try{
            Bitmap bmp=null;
            try(InputStream is=getContentResolver().openInputStream(uri)){
                bmp=BitmapFactory.decodeStream(is);
            }
            if(bmp!=null){
                onImageCapturedForScan(bmp);
            }else{
                Toast.makeText(this,"تعذر تحميل الصورة المختارة",Toast.LENGTH_SHORT).show();
            }
        }catch(Exception e){
            Toast.makeText(this,"خطأ أثناء فتح الصورة: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    Bitmap scaleDownBitmap(Bitmap src,int maxDim){
        int w=src.getWidth(),h=src.getHeight();
        if(w<=maxDim&&h<=maxDim) return src;
        float ratio=Math.min((float)maxDim/w,(float)maxDim/h);
        int nw=Math.round(w*ratio),nh=Math.round(h*ratio);
        return Bitmap.createScaledBitmap(src,Math.max(1,nw),Math.max(1,nh),true);
    }

    Bitmap rotateBitmap(Bitmap src,float angle){
        if(angle==0) return src;
        Matrix m=new Matrix();
        m.postRotate(angle);
        return Bitmap.createBitmap(src,0,0,src.getWidth(),src.getHeight(),m,true);
    }

    Bitmap autoCropDocument(Bitmap src){
        if(src==null) return null;
        int w=src.getWidth(),h=src.getHeight();
        if(w<50||h<50) return src;
        // تقليم الحواف البسيطة لتوسيط محتوى المستند بدقة
        int cropMarginX=Math.max(0,(int)(w*0.02f));
        int cropMarginY=Math.max(0,(int)(h*0.02f));
        int newW=w-cropMarginX*2,newH=h-cropMarginY*2;
        if(newW<=10||newH<=10) return src;
        return Bitmap.createBitmap(src,cropMarginX,cropMarginY,newW,newH);
    }

    Bitmap applyCamScannerFilter(Bitmap src,String mode){
        if(src==null) return null;
        int w=src.getWidth(),h=src.getHeight();
        if("original".equals(mode)){
            return src.copy(src.getConfig(),true);
        }
        Bitmap out=Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888);
        int[] pixels=new int[w*h];
        src.getPixels(pixels,0,w,0,0,w,h);

        if("bw".equals(mode)){
            // تحويل أبيض وأسود فائق الوضوح للنصوص والأرقام
            for(int i=0;i<pixels.length;i++){
                int c=pixels[i];
                int r=(c>>16)&0xFF,g=(c>>8)&0xFF,b=c&0xFF;
                int lum=(r*77+g*150+b*29)>>8;
                int val=lum>135?255:0;
                pixels[i]=0xFF000000|(val<<16)|(val<<8)|val;
            }
        }else if("gray".equals(mode)){
            // تدرج رمادي ناعم
            for(int i=0;i<pixels.length;i++){
                int c=pixels[i];
                int r=(c>>16)&0xFF,g=(c>>8)&0xFF,b=c&0xFF;
                int lum=(r*77+g*150+b*29)>>8;
                pixels[i]=0xFF000000|(lum<<16)|(lum<<8)|lum;
            }
        }else{
            // فلتر سحري "Magic Color" لتحسين التباين وإبراز الفاتورة مثل CamScanner
            for(int i=0;i<pixels.length;i++){
                int c=pixels[i];
                int r=(c>>16)&0xFF,g=(c>>8)&0xFF,b=c&0xFF;
                int lum=(r*77+g*150+b*29)>>8;
                // تفتيح الخلفية لتصبح بيضاء نظيفة وتغميق الحبر والخطوط
                float factor=lum>150?1.28f:0.82f;
                int nr=Math.min(255,Math.max(0,(int)(r*factor)));
                int ng=Math.min(255,Math.max(0,(int)(g*factor)));
                int nb=Math.min(255,Math.max(0,(int)(b*factor)));
                // تعزيز الوضوح العام
                if(lum>180){ nr=Math.min(255,nr+25); ng=Math.min(255,ng+25); nb=Math.min(255,nb+25); }
                pixels[i]=0xFF000000|(nr<<16)|(ng<<8)|nb;
            }
        }
        out.setPixels(pixels,0,w,0,0,w,h);
        return out;
    }

    void onImageCapturedForScan(Bitmap raw){
        scanRawBitmap=autoCropDocument(scaleDownBitmap(raw,1400));
        scanRotation=0;
        scanFilterMode="magic";
        showScanProcessingDialog();
    }

    void showScanProcessingDialog(){
        if(scanRawBitmap==null){
            Toast.makeText(this,"لا توجد صورة لمعالجتها",Toast.LENGTH_SHORT).show();
            return;
        }
        Dialog dlg=new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(BG);
        box.setPadding(dp(12),dp(10),dp(12),dp(10));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        // Header
        LinearLayout header=new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setGravity(Gravity.CENTER_VERTICAL);
        Button closeBtn=button("✕");
        closeBtn.setTextColor(Color.WHITE); closeBtn.setBackgroundColor(RED);
        closeBtn.setOnClickListener(v->dlg.dismiss());
        header.addView(closeBtn,new LinearLayout.LayoutParams(dp(44),dp(40)));

        TextView titleTv=tv("🪄 معالجة وتحسين الفاتورة",16);
        titleTv.setTextColor(GREEN); titleTv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        LinearLayout.LayoutParams tlp=new LinearLayout.LayoutParams(0,dp(40),1);
        tlp.setMargins(dp(6),0,0,0);
        header.addView(titleTv,tlp);
        box.addView(header,new LinearLayout.LayoutParams(-1,dp(48)));

        // Preview Image
        ImageView preview=new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackground(outlined(Color.BLACK,1,8));
        preview.setPadding(dp(2),dp(2),dp(2),dp(2));
        box.addView(preview,new LinearLayout.LayoutParams(-1,dp(230)));

        // Filter Mode Buttons
        LinearLayout filtersRow=new LinearLayout(this);
        filtersRow.setOrientation(LinearLayout.HORIZONTAL);
        filtersRow.setGravity(Gravity.CENTER);
        filtersRow.setPadding(0,dp(4),0,dp(4));

        Button btnMagic=action("🪄 سحري",GREEN);
        Button btnBw=action("📄 أبيض/أسود",DARK);
        Button btnGray=action("🔘 رمادي",BLUE);
        Button btnOrig=action("🖼️ أصلي",MUTED);
        Button btnRotate=action("🔄 تدوير 90°",GOLD);

        final Bitmap[] renderedBmp=new Bitmap[]{null};

        Runnable updatePreview=()->{
            Bitmap rot=rotateBitmap(scanRawBitmap,scanRotation);
            Bitmap proc=applyCamScannerFilter(rot,scanFilterMode);
            renderedBmp[0]=proc;
            preview.setImageBitmap(proc);
        };

        btnMagic.setOnClickListener(v->{ scanFilterMode="magic"; updatePreview.run(); });
        btnBw.setOnClickListener(v->{ scanFilterMode="bw"; updatePreview.run(); });
        btnGray.setOnClickListener(v->{ scanFilterMode="gray"; updatePreview.run(); });
        btnOrig.setOnClickListener(v->{ scanFilterMode="original"; updatePreview.run(); });
        btnRotate.setOnClickListener(v->{ scanRotation=(scanRotation+90)%360; updatePreview.run(); });

        filtersRow.addView(btnMagic,new LinearLayout.LayoutParams(0,dp(40),1));
        filtersRow.addView(btnBw,new LinearLayout.LayoutParams(0,dp(40),1));
        filtersRow.addView(btnGray,new LinearLayout.LayoutParams(0,dp(40),1));
        filtersRow.addView(btnOrig,new LinearLayout.LayoutParams(0,dp(40),1));
        filtersRow.addView(btnRotate,new LinearLayout.LayoutParams(0,dp(40),1.1f));
        box.addView(filtersRow,new LinearLayout.LayoutParams(-1,dp(46)));

        updatePreview.run();

        // Fields Scroll
        ScrollView sv=new ScrollView(this);
        LinearLayout fields=new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);
        fields.setPadding(dp(4),dp(4),dp(4),dp(4));

        String defName="فاتورة_"+new SimpleDateFormat("yyyyMMdd_HHmm",Locale.US).format(new Date());
        EditText nameInput=field("اسم الفاتورة أو الوصف");
        nameInput.setText(defName);
        fields.addView(tv("اسم أو وصف الفاتورة:",12),new LinearLayout.LayoutParams(-1,dp(22)));
        fields.addView(nameInput,new LinearLayout.LayoutParams(-1,dp(38)));

        fields.addView(tv("تصنيف الفاتورة:",12),new LinearLayout.LayoutParams(-1,dp(22)));
        String[] categories={"فواتير مبيعات","فواتير شراء","سندات قبض","مصاريف عامة","أخرى"};
        final String[] selectedCat=new String[]{categories[0]};

        LinearLayout catRow=new LinearLayout(this);
        catRow.setOrientation(LinearLayout.HORIZONTAL);
        final Button[] catButtons=new Button[categories.length];
        for(int i=0;i<categories.length;i++){
            final String cat=categories[i];
            Button cb=button(cat);
            cb.setTextSize(10);
            catButtons[i]=cb;
            cb.setOnClickListener(v->{
                selectedCat[0]=cat;
                for(int j=0;j<categories.length;j++){
                    catButtons[j].setTextColor(categories[j].equals(cat)?Color.WHITE:TEXT);
                    catButtons[j].setBackgroundColor(categories[j].equals(cat)?GREEN:Color.rgb(235,238,235));
                }
            });
            catRow.addView(cb,new LinearLayout.LayoutParams(0,dp(36),1));
        }
        catButtons[0].setTextColor(Color.WHITE);
        catButtons[0].setBackgroundColor(GREEN);
        fields.addView(catRow,new LinearLayout.LayoutParams(-1,dp(40)));

        EditText notesInput=field("ملاحظات إضافية (اختياري)");
        fields.addView(tv("ملاحظات:",12),new LinearLayout.LayoutParams(-1,dp(22)));
        fields.addView(notesInput,new LinearLayout.LayoutParams(-1,dp(38)));

        // Save & Share Buttons
        LinearLayout actionsRow=new LinearLayout(this);
        actionsRow.setOrientation(LinearLayout.HORIZONTAL);
        actionsRow.setPadding(0,dp(6),0,0);

        Button saveBtn=action("💾 حفظ في الأرشيف",GREEN);
        saveBtn.setTextSize(13);
        Button shareBtn=action("📤 حفظ ومشاركة",GOLD);
        shareBtn.setTextSize(13);

        saveBtn.setOnClickListener(v->{
            String name=nameInput.getText().toString().trim();
            if(name.isEmpty()) name=defName;
            String cat=selectedCat[0];
            String notes=notesInput.getText().toString().trim();
            String date=db.now();
            String savedPath=saveBitmapToInvoicesDir(renderedBmp[0]);
            if(savedPath!=null){
                String fileName=new File(savedPath).getName();
                db.addScannedInvoice(name,fileName,cat,notes,date,savedPath);
                Toast.makeText(this,"تم حفظ الفاتورة بنجاح في مجلد invoices_images",Toast.LENGTH_LONG).show();
                dlg.dismiss();
                scanner();
            }else{
                Toast.makeText(this,"فشل حفظ ملف الصورة",Toast.LENGTH_SHORT).show();
            }
        });

        shareBtn.setOnClickListener(v->{
            String name=nameInput.getText().toString().trim();
            if(name.isEmpty()) name=defName;
            String cat=selectedCat[0];
            String notes=notesInput.getText().toString().trim();
            String date=db.now();
            String savedPath=saveBitmapToInvoicesDir(renderedBmp[0]);
            if(savedPath!=null){
                String fileName=new File(savedPath).getName();
                db.addScannedInvoice(name,fileName,cat,notes,date,savedPath);
                Toast.makeText(this,"تم حفظ الفاتورة بنجاح",Toast.LENGTH_SHORT).show();
                dlg.dismiss();
                scanner();
                shareScannedInvoice(savedPath,name);
            }
        });

        actionsRow.addView(saveBtn,new LinearLayout.LayoutParams(0,dp(48),1));
        actionsRow.addView(shareBtn,new LinearLayout.LayoutParams(0,dp(48),1));
        fields.addView(actionsRow,new LinearLayout.LayoutParams(-1,dp(56)));

        sv.addView(fields);
        box.addView(sv,new LinearLayout.LayoutParams(-1,0,1));

        dlg.setContentView(box);
        dlg.show();
    }

    String saveBitmapToInvoicesDir(Bitmap bitmap){
        if(bitmap==null) return null;
        try{
            String fileName="invoice_scan_"+System.currentTimeMillis()+".jpg";
            File dest=new File(getInvoicesImagesDir(),fileName);
            try(FileOutputStream fos=new FileOutputStream(dest)){
                bitmap.compress(Bitmap.CompressFormat.JPEG,92,fos);
                fos.flush();
            }
            return dest.getAbsolutePath();
        }catch(Exception e){
            return null;
        }
    }

    void shareScannedInvoice(String filePath,String title){
        try{
            File file=new File(filePath);
            if(!file.exists()){
                Toast.makeText(this,"ملف الصورة غير موجود",Toast.LENGTH_SHORT).show();
                return;
            }
            Uri uri=FileProvider.getUriForFile(this,getPackageName()+".fileprovider",file);
            Intent intent=new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM,uri);
            intent.putExtra(Intent.EXTRA_SUBJECT,title);
            intent.putExtra(Intent.EXTRA_TEXT,"فاتورة ممسوحة ضوئياً: "+title+"\nبقالة العنزي");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(intent,"مشاركة الفاتورة عبر"));
        }catch(Exception e){
            Toast.makeText(this,"تعذر المشاركة: "+e.getMessage(),Toast.LENGTH_SHORT).show();
        }
    }

    void showScannedInvoiceViewer(long id,String name,String fileName,String category,String notes,String date,String imagePath){
        Dialog dlg=new Dialog(this,android.R.style.Theme_DeviceDefault_Light_NoActionBar_Fullscreen);
        LinearLayout box=new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setBackgroundColor(BG);
        box.setPadding(dp(12),dp(10),dp(12),dp(10));
        box.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        LinearLayout top=new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        Button close=button("✕");
        close.setTextColor(Color.WHITE); close.setBackgroundColor(DARK);
        close.setOnClickListener(v->dlg.dismiss());
        top.addView(close,new LinearLayout.LayoutParams(dp(44),dp(40)));

        TextView titleTv=tv(name,16);
        titleTv.setTextColor(GREEN); titleTv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
        top.addView(titleTv,new LinearLayout.LayoutParams(0,dp(40),1));
        box.addView(top,new LinearLayout.LayoutParams(-1,dp(48)));

        ImageView iv=new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        iv.setBackground(outlined(Color.BLACK,1,8));
        File file=new File(imagePath);
        if(file.exists()){
            Bitmap b=BitmapFactory.decodeFile(imagePath);
            if(b!=null) iv.setImageBitmap(b);
        }
        box.addView(iv,new LinearLayout.LayoutParams(-1,0,1));

        LinearLayout details=card();
        details.setPadding(dp(8),dp(6),dp(8),dp(6));
        details.addView(tv("التصنيف: "+category+"  •  التاريخ: "+date,11),new LinearLayout.LayoutParams(-1,dp(22)));
        if(notes!=null&&!notes.trim().isEmpty()){
            details.addView(tv("ملاحظات: "+notes,11),new LinearLayout.LayoutParams(-1,dp(22)));
        }
        box.addView(details,new LinearLayout.LayoutParams(-1,dp(60)));

        LinearLayout bbar=new LinearLayout(this);
        bbar.setOrientation(LinearLayout.HORIZONTAL);
        bbar.setPadding(0,dp(4),0,0);

        Button shareBtn=action("📤 مشاركة الفاتورة",GOLD);
        shareBtn.setOnClickListener(v->shareScannedInvoice(imagePath,name));

        Button delBtn=action("🗑️ حذف",RED);
        delBtn.setOnClickListener(v->{
            new AlertDialog.Builder(this)
                .setTitle("حذف الفاتورة")
                .setMessage("هل أنت متأكد من حذف هذه الفاتورة من الأرشيف؟")
                .setNegativeButton("إلغاء",null)
                .setPositiveButton("حذف",(d,w)->{
                    db.deleteScannedInvoice(id);
                    if(file.exists()) file.delete();
                    Toast.makeText(this,"تم حذف الفاتورة",Toast.LENGTH_SHORT).show();
                    dlg.dismiss();
                    scanner();
                }).show();
        });

        bbar.addView(shareBtn,new LinearLayout.LayoutParams(0,dp(48),1.5f));
        bbar.addView(delBtn,new LinearLayout.LayoutParams(0,dp(48),1f));
        box.addView(bbar,new LinearLayout.LayoutParams(-1,dp(52)));

        dlg.setContentView(box);
        dlg.show();
    }

    void scanner(){
        base("ماسح الفواتير (CamScanner)");
        section("📷 الماسح الضوئي الذكي للفواتير");

        // Action Buttons Row (Camera + Gallery)
        LinearLayout topActions=new LinearLayout(this);
        topActions.setOrientation(LinearLayout.HORIZONTAL);
        topActions.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        Button camBtn=action("📸 التقاط بالكاميرا",GREEN);
        camBtn.setTextSize(13); camBtn.setMaxLines(1); fitInside(camBtn,14f,10f);
        camBtn.setOnClickListener(v->launchScanCamera());

        Button galleryBtn=action("🖼️ اختيار من المعرض",BLUE);
        galleryBtn.setTextSize(13); galleryBtn.setMaxLines(1); fitInside(galleryBtn,14f,10f);
        galleryBtn.setOnClickListener(v->launchScanGallery());

        topActions.addView(camBtn,new LinearLayout.LayoutParams(0,dp(52),1.1f));
        LinearLayout.LayoutParams gp=new LinearLayout.LayoutParams(0,dp(52),1f);
        gp.setMargins(dp(4),0,0,0);
        topActions.addView(galleryBtn,gp);
        content.addView(topActions,new LinearLayout.LayoutParams(-1,dp(56)));
        addSpace(4);

        // Search Field
        EditText search=field("🔍 بحث في أرشيف الفواتير الممسوحة");
        search.setText(scanSearchQuery);
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int start,int count,int after){}
            public void onTextChanged(CharSequence s,int start,int before,int count){
                scanSearchQuery=s.toString().trim();
                refreshScannedList(content);
            }
            public void afterTextChanged(android.text.Editable s){}
        });
        content.addView(search,new LinearLayout.LayoutParams(-1,dp(38)));
        addSpace(3);

        // Category Filter Chips
        String[] cats={"الكل","فواتير مبيعات","فواتير شراء","سندات قبض","أخرى"};
        LinearLayout catFilterRow=new LinearLayout(this);
        catFilterRow.setOrientation(LinearLayout.HORIZONTAL);
        catFilterRow.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

        final Button[] chips=new Button[cats.length];
        for(int i=0;i<cats.length;i++){
            final String cat=cats[i];
            Button chip=button(cat);
            chip.setTextSize(10);
            chips[i]=chip;
            boolean active=cat.equals(scanCategoryFilter);
            chip.setTextColor(active?Color.WHITE:TEXT);
            chip.setBackgroundColor(active?DARK:Color.rgb(230,235,230));
            chip.setOnClickListener(v->{
                scanCategoryFilter=cat;
                for(int j=0;j<cats.length;j++){
                    boolean sel=cats[j].equals(cat);
                    chips[j].setTextColor(sel?Color.WHITE:TEXT);
                    chips[j].setBackgroundColor(sel?DARK:Color.rgb(230,235,230));
                }
                refreshScannedList(content);
            });
            catFilterRow.addView(chip,new LinearLayout.LayoutParams(0,dp(34),1));
        }
        content.addView(catFilterRow,new LinearLayout.LayoutParams(-1,dp(36)));
        addSpace(4);

        // Container for scanned invoices list
        LinearLayout listContainer=new LinearLayout(this);
        listContainer.setTag("scanned_list_container");
        listContainer.setOrientation(LinearLayout.VERTICAL);
        content.addView(listContainer,new LinearLayout.LayoutParams(-1,-2));

        populateScannedInvoices(listContainer);
    }

    void refreshScannedList(LinearLayout parent){
        LinearLayout container=(LinearLayout)parent.findViewWithTag("scanned_list_container");
        if(container!=null){
            container.removeAllViews();
            populateScannedInvoices(container);
        }
    }

    void populateScannedInvoices(LinearLayout container){
        Cursor c=db.scannedInvoices(scanSearchQuery,scanCategoryFilter);
        int count=0;
        if(c!=null){
            while(c.moveToNext()){
                count++;
                long id=c.getLong(0);
                String name=c.getString(1);
                String fileName=c.getString(2);
                String cat=c.getString(3);
                String notes=c.getString(4);
                String date=c.getString(5);
                String imgPath=c.getString(6);

                LinearLayout card=card();
                card.setOrientation(LinearLayout.HORIZONTAL);
                card.setPadding(dp(8),dp(6),dp(8),dp(6));
                card.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);

                // Thumbnail
                ImageView thumb=new ImageView(this);
                thumb.setScaleType(ImageView.ScaleType.CENTER_CROP);
                thumb.setBackground(outlined(Color.rgb(220,225,220),1,6));
                if(imgPath!=null&&new File(imgPath).exists()){
                    Bitmap b=BitmapFactory.decodeFile(imgPath);
                    if(b!=null) thumb.setImageBitmap(b);
                }
                card.addView(thumb,new LinearLayout.LayoutParams(dp(54),dp(54)));

                // Info Column
                LinearLayout info=new LinearLayout(this);
                info.setOrientation(LinearLayout.VERTICAL);
                info.setPadding(dp(8),0,dp(8),0);

                TextView nameTv=tv(name,13);
                nameTv.setTextColor(GREEN); nameTv.setTypeface(Typeface.DEFAULT,Typeface.BOLD);
                nameTv.setMaxLines(1);
                info.addView(nameTv,new LinearLayout.LayoutParams(-1,dp(22)));

                TextView subTv=tv("🏷️ "+(cat==null?"عام":cat)+"  •  📅 "+date,10);
                subTv.setTextColor(MUTED); subTv.setMaxLines(1);
                info.addView(subTv,new LinearLayout.LayoutParams(-1,dp(18)));

                if(notes!=null&&!notes.trim().isEmpty()){
                    TextView noteTv=tv("📝 "+notes,10);
                    noteTv.setTextColor(TEXT); noteTv.setMaxLines(1);
                    info.addView(noteTv,new LinearLayout.LayoutParams(-1,dp(16)));
                }
                card.addView(info,new LinearLayout.LayoutParams(0,-2,1));

                // Quick Actions Column
                LinearLayout actions=new LinearLayout(this);
                actions.setOrientation(LinearLayout.HORIZONTAL);
                actions.setGravity(Gravity.CENTER_VERTICAL);

                Button viewBtn=button("👁️");
                viewBtn.setTextSize(14);
                viewBtn.setContentDescription("عرض الفاتورة");
                viewBtn.setOnClickListener(v->showScannedInvoiceViewer(id,name,fileName,cat,notes,date,imgPath));
                actions.addView(viewBtn,new LinearLayout.LayoutParams(dp(38),dp(38)));

                Button shareBtn=button("📤");
                shareBtn.setTextSize(14);
                shareBtn.setContentDescription("مشاركة الفاتورة");
                shareBtn.setOnClickListener(v->shareScannedInvoice(imgPath,name));
                actions.addView(shareBtn,new LinearLayout.LayoutParams(dp(38),dp(38)));

                card.addView(actions,new LinearLayout.LayoutParams(-2,-2));

                LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,-2);
                lp.setMargins(0,0,0,dp(6));
                container.addView(card,lp);
            }
            c.close();
        }

        if(count==0){
            LinearLayout emptyBox=card();
            emptyBox.setOrientation(LinearLayout.VERTICAL);
            emptyBox.setPadding(dp(16),dp(20),dp(16),dp(20));
            emptyBox.setGravity(Gravity.CENTER);

            TextView emptyIcon=tv("📄",32);
            emptyIcon.setGravity(Gravity.CENTER);
            emptyBox.addView(emptyIcon,new LinearLayout.LayoutParams(-1,dp(45)));

            TextView emptyText=tv("لا توجد فواتير ممسوحة ضوئياً حتى الآن",13);
            emptyText.setTextColor(MUTED); emptyText.setGravity(Gravity.CENTER);
            emptyBox.addView(emptyText,new LinearLayout.LayoutParams(-1,dp(26)));

            TextView emptySub=tv("اضغط على 'التقاط بالكاميرا' لتصوير فاتورة واقتصاصها وتحسين وضوحها تلقائياً",11);
            emptySub.setTextColor(MUTED); emptySub.setGravity(Gravity.CENTER);
            emptyBox.addView(emptySub,new LinearLayout.LayoutParams(-1,dp(36)));

            container.addView(emptyBox,new LinearLayout.LayoutParams(-1,-2));
        }
    }

    static class DB extends SQLiteOpenHelper{
        DB(Context c){super(c,"enezi.db",null,11);}
        public void onCreate(SQLiteDatabase d){create(d);}
        void create(SQLiteDatabase d){
            d.execSQL("CREATE TABLE customers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT)");
            d.execSQL("CREATE TABLE invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,no TEXT,customer TEXT,total REAL,paid REAL DEFAULT 0,date TEXT)");
            d.execSQL("CREATE TABLE transactions(id INTEGER PRIMARY KEY AUTOINCREMENT,customer_id INTEGER,amount REAL,details TEXT,type INTEGER,date TEXT)");
            d.execSQL("CREATE TABLE items(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT,qty REAL,min_qty REAL,cost REAL DEFAULT 0,sale REAL DEFAULT 0)");
            d.execSQL("CREATE TABLE invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER,name TEXT,qty REAL,total REAL)");
            d.execSQL("CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT)");
            d.execSQL("CREATE TABLE IF NOT EXISTS purchase_invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,no TEXT,supplier TEXT,total REAL,paid REAL DEFAULT 0,date TEXT)");
            d.execSQL("CREATE TABLE IF NOT EXISTS purchase_items(id INTEGER PRIMARY KEY AUTOINCREMENT,purchase_id INTEGER,name TEXT,qty REAL,cost REAL,sale REAL,total REAL)");
            d.execSQL("CREATE TABLE IF NOT EXISTS note_pages(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,date TEXT)");
            d.execSQL("CREATE TABLE IF NOT EXISTS note_items(id INTEGER PRIMARY KEY AUTOINCREMENT,page_id INTEGER,side INTEGER,name TEXT,qty REAL,position INTEGER)");
            d.execSQL("CREATE TABLE IF NOT EXISTS scanned_invoices(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, file_name TEXT, category TEXT, notes TEXT, date TEXT, image_path TEXT)");
        }
        public void onUpgrade(SQLiteDatabase d,int o,int n){
            if(o<6){try{d.execSQL("ALTER TABLE customers ADD COLUMN phone TEXT");}catch(Exception ignored){}}
            if(o<7){try{d.execSQL("ALTER TABLE invoices ADD COLUMN paid REAL DEFAULT 0");}catch(Exception ignored){}}
            if(o<2){try{d.execSQL("ALTER TABLE invoices ADD COLUMN date TEXT");}catch(Exception ignored){}}
            if(o<5){d.execSQL("CREATE TABLE IF NOT EXISTS invoice_items(id INTEGER PRIMARY KEY AUTOINCREMENT,invoice_id INTEGER,name TEXT,qty REAL,total REAL)");}
            if(o<8){try{d.execSQL("ALTER TABLE items ADD COLUMN cost REAL DEFAULT 0");}catch(Exception ignored){}try{d.execSQL("ALTER TABLE items ADD COLUMN sale REAL DEFAULT 0");}catch(Exception ignored){}}
            if(o<9){d.execSQL("CREATE TABLE IF NOT EXISTS suppliers(id INTEGER PRIMARY KEY AUTOINCREMENT,name TEXT NOT NULL,phone TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS purchase_invoices(id INTEGER PRIMARY KEY AUTOINCREMENT,no TEXT,supplier TEXT,total REAL,paid REAL DEFAULT 0,date TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS purchase_items(id INTEGER PRIMARY KEY AUTOINCREMENT,purchase_id INTEGER,name TEXT,qty REAL,cost REAL,sale REAL,total REAL)");}
            if(o<10){d.execSQL("CREATE TABLE IF NOT EXISTS note_pages(id INTEGER PRIMARY KEY AUTOINCREMENT,title TEXT,date TEXT)");d.execSQL("CREATE TABLE IF NOT EXISTS note_items(id INTEGER PRIMARY KEY AUTOINCREMENT,page_id INTEGER,side INTEGER,name TEXT,qty REAL,position INTEGER);");}
            if(o<11){d.execSQL("CREATE TABLE IF NOT EXISTS scanned_invoices(id INTEGER PRIMARY KEY AUTOINCREMENT, name TEXT NOT NULL, file_name TEXT, category TEXT, notes TEXT, date TEXT, image_path TEXT);");}
        }
        long addScannedInvoice(String name,String fileName,String category,String notes,String date,String imagePath){
            ContentValues v=new ContentValues();
            v.put("name",name);
            v.put("file_name",fileName);
            v.put("category",category);
            v.put("notes",notes);
            v.put("date",date);
            v.put("image_path",imagePath);
            return getWritableDatabase().insert("scanned_invoices",null,v);
        }
        Cursor scannedInvoices(String q,String category){
            String sel="";
            ArrayList<String> args=new ArrayList<>();
            if(q!=null&&!q.trim().isEmpty()){
                sel+="(name LIKE ? OR notes LIKE ?)";
                args.add("%"+q+"%");
                args.add("%"+q+"%");
            }
            if(category!=null&&!category.equals("الكل")&&!category.trim().isEmpty()){
                if(!sel.isEmpty()) sel+=" AND ";
                sel+="category=?";
                args.add(category);
            }
            return getReadableDatabase().query("scanned_invoices",
                new String[]{"id","name","file_name","category","notes","date","image_path"},
                sel.isEmpty()?null:sel,
                args.isEmpty()?null:args.toArray(new String[0]),
                null,null,"datetime(date) DESC, id DESC");
        }
        void deleteScannedInvoice(long id){
            if(id>0) getWritableDatabase().delete("scanned_invoices","id=?",new String[]{String.valueOf(id)});
        }
        int scannedInvoiceCount(){
            Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM scanned_invoices",null);
            int x=c.moveToFirst()?c.getInt(0):0;
            c.close();
            return x;
        }
        long createNotePage(String title,String date){ContentValues v=new ContentValues();v.put("title",title);v.put("date",date);return getWritableDatabase().insert("note_pages",null,v);}
        void touchNotePage(long id){if(id>0){ContentValues v=new ContentValues();v.put("date",now());getWritableDatabase().update("note_pages",v,"id=?",new String[]{String.valueOf(id)});}}
        int nextNotePosition(long pageId,int side){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(position),0)+1 FROM note_items WHERE page_id=? AND side=?",new String[]{String.valueOf(pageId),String.valueOf(side)});int x=c.moveToFirst()?c.getInt(0):1;c.close();return x;}
        void addNoteItem(long pageId,String name,double qty,int side){ContentValues v=new ContentValues();v.put("page_id",pageId);v.put("side",side);v.put("name",name);v.put("qty",qty);v.put("position",nextNotePosition(pageId,side));getWritableDatabase().insert("note_items",null,v);touchNotePage(pageId);}
        void loadNoteItems(long pageId,ArrayList<NoteItem> left,ArrayList<NoteItem> right){Cursor c=getReadableDatabase().rawQuery("SELECT side,name,qty FROM note_items WHERE page_id=? ORDER BY side,position,id",new String[]{String.valueOf(pageId)});while(c.moveToNext()){NoteItem x=new NoteItem(c.getString(1),c.getDouble(2),c.getInt(0));if(x.side==1)left.add(x);else right.add(x);}c.close();}
        void clearNoteItems(long pageId){getWritableDatabase().delete("note_items","page_id=?",new String[]{String.valueOf(pageId)});touchNotePage(pageId);}
        void deleteNoteItem(long pageId,String name,double qty,int side){SQLiteDatabase d=getWritableDatabase();d.delete("note_items","id=(SELECT id FROM note_items WHERE page_id=? AND side=? AND name=? AND qty=? ORDER BY position,id LIMIT 1)",new String[]{String.valueOf(pageId),String.valueOf(side),name,String.valueOf(qty)});touchNotePage(pageId);}
        Cursor notePages(){return getReadableDatabase().rawQuery("SELECT p.id,p.title,p.date,COUNT(i.id) FROM note_pages p LEFT JOIN note_items i ON i.page_id=p.id GROUP BY p.id ORDER BY datetime(p.date) DESC,p.id DESC",null);}
        String now(){return new SimpleDateFormat("yyyy-MM-dd HH:mm",Locale.US).format(new Date());}
        long customer(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=?",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);return getWritableDatabase().insert("customers",null,v);}
        void updateCustomer(long id,String oldName,String newName,String phone){
            SQLiteDatabase d=getWritableDatabase(); ContentValues v=new ContentValues();v.put("name",newName);v.put("phone",phone);
            d.update("customers",v,"id=?",new String[]{String.valueOf(id)});
            if(oldName!=null&&!oldName.equals(newName)){ContentValues iv=new ContentValues();iv.put("customer",newName);d.update("invoices",iv,"customer=?",new String[]{oldName});}
        }
        void addCustomer(String n,String p){ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);getWritableDatabase().insert("customers",null,v);}
        long addInvoice(String no,String c,double t,double paid,String date){ContentValues v=new ContentValues();v.put("no",no);v.put("customer",c);v.put("total",t);v.put("paid",paid);v.put("date",date);return getWritableDatabase().insert("invoices",null,v);}
        void addTransaction(long id,double a,String d,int type,String date){if(id<1)return;ContentValues v=new ContentValues();v.put("customer_id",id);v.put("amount",a);v.put("details",d);v.put("type",type);v.put("date",date);getWritableDatabase().insert("transactions",null,v);}
        double balance(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(CASE WHEN type=1 THEN amount ELSE -amount END),0) FROM transactions WHERE customer_id=?",new String[]{String.valueOf(id)});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        Cursor customers(String q){return getReadableDatabase().rawQuery("SELECT id,name,COALESCE(phone,'') FROM customers WHERE name LIKE ? OR phone LIKE ? ORDER BY name",new String[]{"%"+q+"%","%"+q+"%"});}
        Cursor transactions(long id){return getReadableDatabase().rawQuery("SELECT id,date,details,amount,type FROM transactions WHERE customer_id=? ORDER BY datetime(date) DESC, id DESC",new String[]{String.valueOf(id)});}
        int nextPurchaseNo(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(CAST(no AS INTEGER)),0)+1 FROM purchase_invoices",null);int x=c.moveToFirst()?c.getInt(0):1;c.close();return x;}
        String[] supplierNames(){Cursor c=getReadableDatabase().rawQuery("SELECT name FROM suppliers ORDER BY name",null);ArrayList<String>a=new ArrayList<>();while(c.moveToNext())a.add(c.getString(0));c.close();return a.toArray(new String[0]);}
        long supplier(String n,String p){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM suppliers WHERE name=? LIMIT 1",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);return getWritableDatabase().insert("suppliers",null,v);}
        long addPurchase(String no,String supplier,double total,String date){ContentValues v=new ContentValues();v.put("no",no);v.put("supplier",supplier);v.put("total",total);v.put("date",date);return getWritableDatabase().insert("purchase_invoices",null,v);}
        void replacePurchaseLines(long id,ArrayList<PurchaseLine> ls){SQLiteDatabase d=getWritableDatabase();for(PurchaseLine l:ls){ContentValues v=new ContentValues();v.put("purchase_id",id);v.put("name",l.name);v.put("qty",l.qty);v.put("cost",l.cost);v.put("sale",l.sale);v.put("total",l.total);d.insert("purchase_items",null,v);}}
        void updateStockFromPurchase(ArrayList<PurchaseLine> ls){SQLiteDatabase d=getWritableDatabase();for(PurchaseLine l:ls){Cursor c=d.rawQuery("SELECT id,qty FROM items WHERE name=? LIMIT 1",new String[]{l.name});if(c.moveToFirst()){long id=c.getLong(0);double q=c.getDouble(1);c.close();ContentValues v=new ContentValues();v.put("qty",q+l.qty);v.put("cost",l.cost);v.put("sale",l.sale);d.update("items",v,"id=?",new String[]{String.valueOf(id)});}else{c.close();ContentValues v=new ContentValues();v.put("name",l.name);v.put("qty",l.qty);v.put("min_qty",0);v.put("cost",l.cost);v.put("sale",l.sale);d.insert("items",null,v);}}}
        String[] itemNames(){Cursor c=getReadableDatabase().rawQuery("SELECT name FROM items ORDER BY name",null);ArrayList<String>a=new ArrayList<>();while(c.moveToNext())a.add(c.getString(0));c.close();return a.toArray(new String[0]);}
        Cursor items(){return getReadableDatabase().rawQuery("SELECT id,name,qty,min_qty FROM items ORDER BY name",null);}
        boolean itemExists(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM items WHERE name=? LIMIT 1",new String[]{n});boolean x=c.moveToFirst();c.close();return x;}
        void addItem(String n,double q,double m){if(n.isEmpty()||q<0||m<0)throw new IllegalArgumentException();SQLiteDatabase d=getWritableDatabase();Cursor c=d.rawQuery("SELECT id FROM items WHERE name=? LIMIT 1",new String[]{n});if(c.moveToFirst()){long id=c.getLong(0);c.close();ContentValues v=new ContentValues();v.put("qty",q);v.put("min_qty",m);d.update("items",v,"id=?",new String[]{String.valueOf(id)});return;}c.close();ContentValues v=new ContentValues();v.put("name",n);v.put("qty",q);v.put("min_qty",m);d.insert("items",null,v);}
        void updateItem(long id,String n,double q,double m){if(id<1||n==null||n.trim().isEmpty()||q<0||m<0)throw new IllegalArgumentException();ContentValues v=new ContentValues();v.put("name",n.trim());v.put("qty",q);v.put("min_qty",m);getWritableDatabase().update("items",v,"id=?",new String[]{String.valueOf(id)});}
        void deleteItem(long id){if(id>0)getWritableDatabase().delete("items","id=?",new String[]{String.valueOf(id)});}
        int transactionCount(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM transactions WHERE customer_id=?",new String[]{String.valueOf(id)});int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        String invoiceNoFromTransaction(String details){if(details==null)return "";String p="فاتورة مبيعات رقم ";return details.startsWith(p)?details.substring(p.length()).trim():"";}
        String invoiceCompactDetails(String no){Cursor c=getReadableDatabase().rawQuery("SELECT name,qty,total FROM invoice_items WHERE invoice_id=(SELECT id FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1) ORDER BY id",new String[]{no});StringBuilder s=new StringBuilder("تفاصيل: ");int n=0;while(c.moveToNext()&&n<6){if(n>0)s.append(" • ");s.append(c.getString(0)).append(" × ").append(fmt(c.getDouble(1))).append(" = ").append(fmt(c.getDouble(2)));n++;}c.close();return n==0?"تفاصيل الفاتورة غير متاحة":s.toString();}
        Cursor purchaseLines(long id){return getReadableDatabase().rawQuery("SELECT id,name,qty,cost,sale,total FROM purchase_items WHERE purchase_id=? ORDER BY id",new String[]{String.valueOf(id)});}
        String purchaseSupplier(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(supplier,'') FROM purchase_invoices WHERE id=?",new String[]{String.valueOf(id)});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        String supplierPhoneByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(phone,'') FROM suppliers WHERE name=? ORDER BY id DESC LIMIT 1",new String[]{n==null?"":n});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        Cursor invoices(){return getReadableDatabase().rawQuery("SELECT id,no,customer,total,date FROM invoices ORDER BY datetime(date) DESC, id DESC LIMIT 100",null);}
        Cursor recentActivity(){
            android.database.MatrixCursor out=new android.database.MatrixCursor(
                new String[]{"kind","ref","title","amount","date","sort_id","operation_type"});
            ArrayList<Object[]> rows=new ArrayList<>();
            SQLiteDatabase d=getReadableDatabase();
            Cursor inv=null,tr=null;
            try{
                inv=d.rawQuery("SELECT id,no,customer,COALESCE(total,0),COALESCE(date,''),id FROM invoices",null);
                while(inv.moveToNext()){
                    long id=inv.getLong(0);
                    String no=inv.getString(1)==null?"":inv.getString(1);
                    String customer=inv.getString(2);
                    if(customer==null||customer.trim().isEmpty())customer="نقدي";
                    rows.add(new Object[]{1,no,"فاتورة "+no+" • "+customer,inv.getDouble(3),inv.getString(4),id,0});
                }
            }finally{if(inv!=null)inv.close();}
            try{
                tr=d.rawQuery("SELECT t.id,t.details,COALESCE(t.amount,0),COALESCE(t.date,''),t.customer_id,c.name,t.type FROM transactions t LEFT JOIN customers c ON c.id=t.customer_id",null);
                while(tr.moveToNext()){
                    String details=tr.getString(1);
                    // قيد الفاتورة يُعرض مرة واحدة كفاتورة، وليس كحركة إضافية.
                    if(details!=null && (details.startsWith("فاتورة مبيعات رقم ") || details.startsWith("دفعة فاتورة رقم "))) continue;
                    String customer=tr.getString(5);
                    if(customer==null)customer="";
                    String title=(details==null||details.trim().isEmpty()?"عملية مالية":details.trim());
                    if(!customer.trim().isEmpty()) title+=" • "+customer.trim();
                    rows.add(new Object[]{2,String.valueOf(tr.getLong(0)),title,tr.getDouble(2),tr.getString(3),tr.getLong(0),tr.getInt(6)});
                }
            }finally{if(tr!=null)tr.close();}
            Cursor pinv=null;
            try{
                pinv=d.rawQuery("SELECT id,no,supplier,COALESCE(total,0),COALESCE(date,''),id FROM purchase_invoices",null);
                while(pinv.moveToNext()){
                    long id=pinv.getLong(0);String no=pinv.getString(1)==null?"":pinv.getString(1);String supplier=pinv.getString(2);
                    if(supplier==null||supplier.trim().isEmpty())supplier="بدون مورد";
                    rows.add(new Object[]{3,no,"فاتورة شراء "+no+" • "+supplier,pinv.getDouble(3),pinv.getString(4),id,0});
                }
            }finally{if(pinv!=null)pinv.close();}
            Collections.sort(rows,(a,b)->{
                String da=(String)a[4], dbb=(String)b[4];
                int x=dbb.compareTo(da);
                if(x!=0)return x;
                return Long.compare((Long)b[5],(Long)a[5]);
            });
            int n=Math.min(200,rows.size());
            for(int i=0;i<n;i++) out.addRow(rows.get(i));
            return out;
        }
        long transactionIdForInvoice(String no){
            Cursor c=getReadableDatabase().rawQuery("SELECT id FROM transactions WHERE details=? ORDER BY id DESC LIMIT 1",new String[]{"فاتورة مبيعات رقم "+no});
            long x=c.moveToFirst()?c.getLong(0):-1;c.close();return x;
        }
        double balanceAfterTransaction(long tid){
            Cursor c=getReadableDatabase().rawQuery("SELECT customer_id,type,amount FROM transactions WHERE id=?",new String[]{String.valueOf(tid)});
            if(!c.moveToFirst()){c.close();return 0;}
            long cid=c.getLong(0);int type=c.getInt(1);double amount=c.getDouble(2);c.close();
            double current=balance(cid);
            // الحساب الحالي = الرصيد بعد الحركة. نعيد طرح/إضافة الحركات الأحدث حتى لحظة العملية.
            Cursor newer=getReadableDatabase().rawQuery("SELECT type,amount FROM transactions WHERE customer_id=? AND id>?",new String[]{String.valueOf(cid),String.valueOf(tid)});
            while(newer.moveToNext()) current-=(newer.getInt(0)==1?newer.getDouble(1):-newer.getDouble(1));
            newer.close();
            return current;
        }
        String customerNameById(long id){
            Cursor c=getReadableDatabase().rawQuery("SELECT name FROM customers WHERE id=?",new String[]{String.valueOf(id)});
            String x=c.moveToFirst()?c.getString(0):"";c.close();return x;
        }
        double invoicePaid(long id){
            Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(paid,0) FROM invoices WHERE id=?",new String[]{String.valueOf(id)});
            double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;
        }
        String invoiceDate(long id){
            Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(date,'') FROM invoices WHERE id=?",new String[]{String.valueOf(id)});
            String x=c.moveToFirst()?c.getString(0):"";c.close();return x;
        }
        int invoiceCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM invoices",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        int customerCount(){Cursor c=getReadableDatabase().rawQuery("SELECT COUNT(*) FROM customers",null);int x=c.moveToFirst()?c.getInt(0):0;c.close();return x;}
        double sales(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(SUM(total),0) FROM invoices",null);double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        String[] customerNames(){Cursor c=getReadableDatabase().rawQuery("SELECT name FROM customers ORDER BY name",null);ArrayList<String>a=new ArrayList<>();while(c.moveToNext())a.add(c.getString(0));c.close();return a.toArray(new String[0]);}
        long customerIdByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=? ORDER BY id DESC LIMIT 1",new String[]{n});long x=c.moveToFirst()?c.getLong(0):-1;c.close();return x;}
        double invoiceTotal(long id){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(total,0) FROM invoices WHERE id=?",new String[]{String.valueOf(id)});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        double invoiceTotalByNo(String no){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(total,0) FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1",new String[]{no});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        double invoicePaidByNo(String no){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(paid,0) FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1",new String[]{no});double x=c.moveToFirst()?c.getDouble(0):0;c.close();return x;}
        String phoneByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(phone,'') FROM customers WHERE name=? LIMIT 1",new String[]{n});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        long customer(String n,String p){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=?",new String[]{n});if(c.moveToFirst()){long x=c.getLong(0);c.close();ContentValues v=new ContentValues();v.put("phone",p);getWritableDatabase().update("customers",v,"id=?",new String[]{String.valueOf(x)});return x;}c.close();ContentValues v=new ContentValues();v.put("name",n);v.put("phone",p);return getWritableDatabase().insert("customers",null,v);}
        double balanceByName(String n){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM customers WHERE name=? ORDER BY id DESC LIMIT 1",new String[]{n});if(!c.moveToFirst()){c.close();return 0;}long id=c.getLong(0);c.close();return balance(id);}
        String invoiceNo(long id){Cursor c=getReadableDatabase().rawQuery("SELECT no FROM invoices WHERE id=?",new String[]{String.valueOf(id)});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        String invoiceCustomer(long id){Cursor c=getReadableDatabase().rawQuery("SELECT customer FROM invoices WHERE id=?",new String[]{String.valueOf(id)});String x=c.moveToFirst()?c.getString(0):"";c.close();return x==null?"":x;}
        void updateInvoice(long id,String no,String customer,double total,double paid,String date){ContentValues v=new ContentValues();v.put("no",no);v.put("customer",customer);v.put("total",total);v.put("paid",paid);v.put("date",date);getWritableDatabase().update("invoices",v,"id=?",new String[]{String.valueOf(id)});}
        Cursor invoiceLines(long id){return getReadableDatabase().rawQuery("SELECT id,name,qty,total FROM invoice_items WHERE invoice_id=? ORDER BY id",new String[]{String.valueOf(id)});}
        void replaceInvoiceLines(long id,ArrayList<Line> ls){SQLiteDatabase d=getWritableDatabase();d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(id)});for(Line l:ls){ContentValues v=new ContentValues();v.put("invoice_id",id);v.put("name",l.name);v.put("qty",l.qty);v.put("total",l.total);d.insert("invoice_items",null,v);}}
        void deleteInvoice(long id){String no=invoiceNo(id);deleteInvoiceTransactions(no);SQLiteDatabase d=getWritableDatabase();d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(id)});d.delete("invoices","id=?",new String[]{String.valueOf(id)});}
        void deleteInvoiceTransaction(String no){deleteInvoiceTransactions(no);}
        void deleteInvoiceTransactions(String no){
            SQLiteDatabase d=getWritableDatabase();
            d.delete("transactions","details=? OR details=?",new String[]{"فاتورة مبيعات رقم "+no,"دفعة فاتورة رقم "+no});
        }
        void addPaymentTransaction(long id,double a,String details,String date){if(id<1||a<=0)return;addTransaction(id,a,details,0,date);}
        void addTransactionOnce(long id,double a,String details,String date){if(id>0)addTransaction(id,a,details,1,date);}
        int nextInvoice(){Cursor c=getReadableDatabase().rawQuery("SELECT COALESCE(MAX(CAST(no AS INTEGER)),0)+1 FROM invoices",null);int x=c.moveToFirst()?c.getInt(0):1;c.close();return x;}
        long invoiceIdByNo(String no){Cursor c=getReadableDatabase().rawQuery("SELECT id FROM invoices WHERE no=? ORDER BY id DESC LIMIT 1",new String[]{no});long x=c.moveToFirst()?c.getLong(0):-1;c.close();return x;}
        Cursor transactionById(long id){return getReadableDatabase().rawQuery("SELECT id,customer_id,date,details,amount,type FROM transactions WHERE id=?",new String[]{String.valueOf(id)});}
        void updateTransaction(long id,double amount,String details,int type,String date){ContentValues v=new ContentValues();v.put("amount",amount);v.put("details",details);v.put("type",type);v.put("date",date);getWritableDatabase().update("transactions",v,"id=?",new String[]{String.valueOf(id)});}
        void deleteTransaction(long id){getWritableDatabase().delete("transactions","id=?",new String[]{String.valueOf(id)});}
        void deleteCustomer(long id){
            SQLiteDatabase d=getWritableDatabase();
            Cursor c=d.rawQuery("SELECT id FROM invoices WHERE customer=(SELECT name FROM customers WHERE id=?)",new String[]{String.valueOf(id)});
            ArrayList<Long> invoiceIds=new ArrayList<>();while(c.moveToNext())invoiceIds.add(c.getLong(0));c.close();
            d.delete("transactions","customer_id=?",new String[]{String.valueOf(id)});
            for(Long iid:invoiceIds)d.delete("invoice_items","invoice_id=?",new String[]{String.valueOf(iid)});
            d.delete("invoices","customer=(SELECT name FROM customers WHERE id=?)",new String[]{String.valueOf(id)});
            d.delete("customers","id=?",new String[]{String.valueOf(id)});
        }
    }
}