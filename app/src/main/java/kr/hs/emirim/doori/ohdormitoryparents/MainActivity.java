package kr.hs.emirim.doori.ohdormitoryparents;

import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;

import kr.hs.emirim.doori.ohdormitoryparents.FCM.FirebaseInstanceIDService;

public class MainActivity extends BaseActivity {

    private FirebaseDatabase mDatabase;
    String myNumber;
    TextView textView;

   ImageView qrImage;
    private String maxDate="2000.1.1";
    private String maxFBdate;
    private boolean alreadyRecognize=false;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        FirebaseMessaging.getInstance().subscribeToTopic("parentNotice");

        textView=(TextView)findViewById(R.id.text_none_qrcode);
        qrImage=(ImageView) findViewById(R.id.iv_generated_qrcode);

        checkCallPermission();
    }

    @Override
    protected void onRestart() {
        super.onResume();
        checkCallPermission();
    }

    public void checkCallPermission(){
        final TelephonyManager mgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);

        mgr.listen(new PhoneStateListener() {
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                try {
                    String myNumber = mgr.getLine1Number();
                    myNumber = myNumber.replace("+82", "0");
                    Log.e("내 전화번호", myNumber);

                    FirebaseInstanceIDService fcmIDservice=new FirebaseInstanceIDService();
                    fcmIDservice.sendRegistrationToServer(myNumber);

                    getSleepOutInfo(myNumber);
                }catch(Exception e){

                    final Dialog mDialog = new Dialog(MainActivity.this,R.style.MyDialog);
                    mDialog.setContentView(R.layout.dialog);
                    ((TextView)mDialog.findViewById(R.id.dialog_text)).setText("앱 설정에서 전화 권한을 부여해주세요.");
                    mDialog.findViewById(R.id.dialog_button_yes).setOnClickListener(new View.OnClickListener(){
                        @Override
                        public void onClick(View view) {
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                                    .setData(Uri.parse("package:" + MainActivity.this.getPackageName()));
                            MainActivity.this.startActivity(intent);
                            mDialog.dismiss();
                        }
                    });
                    mDialog.show();
                }

                super.onCallStateChanged(state, incomingNumber);
            }
        }, PhoneStateListener.LISTEN_CALL_STATE);

        hideProgressDialog();
    }


    public void generateQRCode(String contents) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            Bitmap bitmap = toBitmap(qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, 150, 150));
            qrImage.setImageBitmap(bitmap);
        } catch (WriterException e) {
            e.printStackTrace();
        }
    }

    public static Bitmap toBitmap(BitMatrix matrix) {
        int height = matrix.getHeight();
        int width = matrix.getWidth();
        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565);
        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                bmp.setPixel(x, y, matrix.get(x, y) ? Color.BLACK : Color.WHITE);
            }
        }
        return bmp;
    }



    public void getSleepOutInfo(final String myNumber){

        mDatabase = FirebaseDatabase.getInstance();

        final DatabaseReference sleepOutRef = mDatabase.getReference("sleep-out");

        showProgressDialog();
        ValueEventListener sleepOutListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot sleepOutData) {
                Iterator<DataSnapshot> sleepOutIterator = sleepOutData.getChildren().iterator();
                //users의 모든 자식들의 key값과 value 값들을 iterator로 참조
                //TODO :외박날짜 최근꺼 가져와서..
                while(sleepOutIterator.hasNext()) {
                    DataSnapshot sleepOut = sleepOutIterator.next();
                    String sleepOutDate = sleepOut.getKey();
                    checkDate(sleepOutDate);
                }

                String send = sleepOutData.child(maxFBdate).child("send").getValue(String.class);
                    if(send==null) send="false";

                    Iterator<DataSnapshot> sleepOutStudentIterator = sleepOutData.child(maxFBdate).getChildren().iterator();
                    String qrcodeContent=null;
                    while (sleepOutStudentIterator.hasNext()) {
                        DataSnapshot sleepOutStudent = sleepOutStudentIterator.next();
                        String studentKey = sleepOutStudent.getKey();

                        if(!studentKey.equals("send") && send.equals("true")) {
                            String parentNumber = sleepOutStudent.child("parentNumber").getValue(String.class);
                            String  recognize= sleepOutStudent.child("recognize").getValue(String.class);
                            parentNumber = parentNumber.replace("-", "");

                                Log.e("부모님 번호", parentNumber);
                                if (myNumber.equals(parentNumber)) {//기기 번호와 부모번호가 같으면
                                    Log.e("기기번호와 부모번호가 같으면", "큐얼코드 생성");
                                    if (recognize.equals("true")) {
                                        Log.e("qr", "이미 인증하셨습니다");
                                        textView.setVisibility(View.VISIBLE);
                                        qrImage.setVisibility(View.GONE);
                                        textView.setText("이미 인증하셨습니다.");
                                        alreadyRecognize=true;
                                    } else {

                                        qrcodeContent = maxFBdate + "/" + studentKey;


                                        qrImage.setVisibility(View.VISIBLE);
                                        textView.setVisibility(View.GONE);
                                        Log.e("qr", qrcodeContent);
                                        generateQRCode(qrcodeContent);

                                        String[] dates = maxFBdate.split("-");
                                        ((TextView) findViewById(R.id.guideText)).setText(dates[0] + "." + dates[1] + "." + dates[2] + ". - " + dates[3] + "." + dates[4] + "." + dates[5] + "\n\n외박인증 큐알코드입니다.");
                                    }
                                }
                            }

                    }
                    if(qrcodeContent==null && !alreadyRecognize){
                        Log.e("TAG", "HERE >3<");
                        textView.setVisibility(View.VISIBLE);
                        textView.setText("자녀의 외박 신청이 없습니다.");
                        qrImage.setVisibility(View.GONE);
                        ((TextView)findViewById(R.id.guideText)).setText("");
                    }

                hideProgressDialog();
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        sleepOutRef.addValueEventListener(sleepOutListener);
    }

    //TODO : 제일최근날짜체크
    public void checkDate(String fbDate) {
        String simpleFBDate=fbDate.replaceFirst("-",".");
        simpleFBDate=simpleFBDate.replaceFirst("-",".");

        int index = simpleFBDate.indexOf("-");
        simpleFBDate = simpleFBDate.substring(0,index);

        Log.e("파베 저장되있는 i날짜",simpleFBDate);
        Log.e("maxDate",maxDate);

        try{
            SimpleDateFormat format = new SimpleDateFormat("yyyy.MM.dd");
            Date max = format.parse(maxDate);

            Date fb = format.parse(simpleFBDate);

            long calDate = max.getTime() - fb.getTime();

            // Date.getTime() 은 해당날짜를 기준으로1970년 00:00:00 부터 몇 초가 흘렀는지를 반환해준다.
            // 이제 24*60*60*1000(각 시간값에 따른 차이점) 을 나눠주면 일수가 나온다.
            long calDateDays = calDate / ( 24*60*60*1000);
            if(calDateDays <= 0) {//max가 더 빠르다면
                maxDate=simpleFBDate;
                maxFBdate=fbDate;
            }

        } catch (ParseException e) {
            Log.e("날짜 파싱에러","서로 타입이 맞지 않음.");
        }

    }

}
