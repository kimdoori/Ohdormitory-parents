package kr.hs.emirim.doori.ohdormitoryparents;

import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.widget.ImageView;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;

import java.util.Iterator;

public class MainActivity extends Activity {

    private FirebaseDatabase mDatabase;
    String myNumber;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        final TelephonyManager mgr = (TelephonyManager) getSystemService(Context.TELEPHONY_SERVICE);
        mgr.listen(new PhoneStateListener(){
            @Override
            public void onCallStateChanged(int state, String incomingNumber) {
                String myNumber=mgr.getLine1Number();
                myNumber = myNumber.replace("+82", "0");

                Log.e("내 전화번호",myNumber);
                getSleepOutInfo(myNumber);
                super.onCallStateChanged(state, incomingNumber);
            }
        },PhoneStateListener.LISTEN_CALL_STATE);


    }

    public void generateRQCode(String contents) {
        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        try {
            Bitmap bitmap = toBitmap(qrCodeWriter.encode(contents, BarcodeFormat.QR_CODE, 350, 350));
            ((ImageView) findViewById(R.id.iv_generated_qrcode)).setImageBitmap(bitmap);
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

        ValueEventListener sleepOutListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot sleepOutData) {
                Iterator<DataSnapshot> sleepOutIterator = sleepOutData.getChildren().iterator();
                //users의 모든 자식들의 key값과 value 값들을 iterator로 참조
                while(sleepOutIterator.hasNext()) {
                    DataSnapshot sleepOut = sleepOutIterator.next();
                    String sleepOutDate = sleepOut.getKey();
                    Log.e("외박 날짜",sleepOutDate);
                    String send=sleepOut.child("send").getValue(String.class);
                    Iterator<DataSnapshot> sleepOutStudentIterator = sleepOut.getChildren().iterator();
                    while (sleepOutStudentIterator.hasNext()) {
                        DataSnapshot sleepOutStudent = sleepOutStudentIterator.next();
                        String studentKey = sleepOutStudent.getKey();

                        if(!studentKey.equals("send") && send.equals("true")) {
                            String parentNumber = sleepOutStudent.child("parentNumber").getValue(String.class);
                            parentNumber = parentNumber.replace("-", "");

                            Log.e("부모님 번호", parentNumber);
                            if (myNumber.equals(parentNumber)) {//기기 번호와 부모번호가 같으면
                                Log.e("기기번호와 부모번호가 같으면", "헤헤");
                                String qrcodeContent = sleepOutDate + studentKey;
                                generateRQCode(qrcodeContent);
                                break;
                            }
                        }
                    }
                }
            }
            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        sleepOutRef.addValueEventListener(sleepOutListener);



    }

}
