package com.example.fansy.audiokeyboard;

import android.app.RemoteInput;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.RectF;
import android.icu.text.SimpleDateFormat;
import android.media.Image;
import android.media.MediaPlayer;
import android.os.Build;
import android.os.Environment;
import android.os.Vibrator;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Paint;

import org.w3c.dom.Text;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;

public class MainActivity extends AppCompatActivity {
    //voice manager
    HashMap<String, int[]> voice = new HashMap<>();
    ArrayList<Integer> myPlayList = new ArrayList<>();
    MediaPlayer current;
    final char KEY_NOT_FOUND = '*';
    final int INIT_MODE_ABSOLUTE = 0;
    final int INIT_MODE_RELATIVE = 1;
    final int INIT_MODE_NOTHING = 2;
    int initMode = INIT_MODE_RELATIVE;
    ImageView keyboard;
    TextView text, candidatesView, readListView,voiceSpeedText;
    Button confirmButton, initModeButton,testMode,speedControl;
    String readList = ""; //current voice list
    String currentWord = ""; //most possible char sequence
    String currentWord2 = ""; //second possible char sequence
    String currentBaseline = "";
    char nowCh = 0; //the most possible char
    char nowCh2 = 0; //the second possible char, '*' if less than 1/10 of the most possible char
    ArrayList<Word> dict = new ArrayList<>();
    ArrayList<Character> seq = new ArrayList<Character>(); //char sequence during the whole touch
    String keys[] = new String[] {"qwertyuiop", "asdfghjkl", "zxcvbnm"};
    String keysNearby[] = new String[26];
    double keysNearbyProb[][] = new double[26][26];
    int key_left[] = new int[26];
    int key_right[] = new int[26];
    int key_top[] = new int[26];
    int key_bottom[] = new int[26];
    int deltaX = 0, deltaY = 0; //translation of XY coordinate

    int voiceSpeed = 50;
    AutoKeyboard autoKeyboard;
    float screen_width_ratio = 1F;
    float screen_height_ratio = 1F;
    //Fuzzy Input Test Var
    float keyboardHeight=570;
    TextView fuzzyInputTestCharShow;
    Button fuzzyInputTestBegin;
    ProgressBar progressBar;
    ListView listView;
    Button testTurnChange;
    TextView testTurn;
    //String fuzzyInputTestStoragePath="/storage/emulated/0/FuzzyInputTestData";
    boolean ifCalDone=false;
    boolean ifSave=false;
    final int KEYBOARD_MODE=0;
    final int FUZZY_INPUT_TEST_MODE=1;
    int MAX_TURN=8;//eight turn
    int MAX_FUZZY_INPUT_TURN=26*MAX_TURN;
    final int FUZZY_INPUT_SOUND_BEGIN=0;//for sound
    final int FUZZY_INPUT_SOUND_NEXT=1;//for sound
    final int FUZZY_INPUT_SOUND_END=2;//
    int activity_mode=KEYBOARD_MODE;
    final int NORMAL=0;
    final int CANCEL=1;
    final int BORDER=2;
    final int CANCEL_BORDER=3;
    int testmode=NORMAL;
    int fuzzyInputTestTurn=0;
    ArrayList<Integer> fuzzyInputTestList=new ArrayList<>();
    //int fuzzyInputTestData[]=new int[MAX_FUZZY_INPUT_TURN+30];//用来记录第一次手指落下的地方
    //in case of the user type too fast,add a redundancy 30
    //double fuzzyInputKeysNearbyProb[][] = new double[26][26];
    ArrayList<Turn> fuzzyInputTestFigerRecord=new ArrayList<>();//用来记录手指的所有移动
    String DataToSave;//记录每一次触摸事件的准确数据
    String DataToShowAndSave;//计算出概率
    //Fuzzy Input test Var
    class Data{
        char target;
        char actual;
        double positionX;
        double positionY;
        int actionType;//ActionMove ActionDown ActionUp
        //ACTION_DOWN==0
        //ACTION_MOVE==2;
        //ACTION_UP==1;
        double timeAfterLastAction;
        int currentSoundNum;//此时正在播放的音频的数量加上等待播放的音频的数量
        Data(char target,char actual,double positionX,double positionY,int actionType,double timeAfterLastAction){
            this.target=target;
            this.actual=actual;
            this.positionX=positionX;
            this.positionY=positionY;
            this.actionType=actionType;
            this.timeAfterLastAction=timeAfterLastAction;
            if(current==null){
                this.currentSoundNum=myPlayList.size();
            }else{
                this.currentSoundNum=1+myPlayList.size();
            }
        }
        Data(){
            this.target='?';
            this.actual='?';
            this.positionX=-1;
            this.positionY=-1;
            this.actionType=-1;
            this.timeAfterLastAction=-1;
            this.currentSoundNum=-1;
        }
        String getData(){
            String data=String.valueOf(target)+" "+String.valueOf(actual)+" "+
                    String.valueOf(positionX)+" "+String.valueOf(positionY)+" "+
                    String.valueOf(actionType)+" "+String.valueOf(timeAfterLastAction)+" "+
                    String.valueOf(currentSoundNum)+"\n";
            return data;
        }
    }
    class Turn {
        Long lastTime;
        int turnIndex;
        char target;
        int upTimes;//用户抬起了多少次手指
        ArrayList<Character> actualTypeIn;//用户实际上键入的值
        ArrayList<Data> data;
        char firstTouch;//除去一开始输到边界外的键入的值
        Turn(int turnIndex,Long lastTime){
            this.lastTime=lastTime;
            this.turnIndex=turnIndex;
            this.target=(char)(fuzzyInputTestList.get(turnIndex%26)+'A');
            this.upTimes=0;
            this.data=new ArrayList<>();
            this.actualTypeIn=new ArrayList<>();
            this.firstTouch=KEY_NOT_FOUND;
        }
        boolean addData(char actual,double positionX,double positionY,int actionType,Long eventTime){
            char upperActual=actual;
            if (upperActual!=KEY_NOT_FOUND)
                upperActual=Character.toUpperCase(upperActual);
            if (this.firstTouch==KEY_NOT_FOUND && upperActual!=KEY_NOT_FOUND)
                this.firstTouch=upperActual;
            this.data.add(new Data(this.target,upperActual,positionX,positionY,actionType,eventTime-this.lastTime));
            this.lastTime=eventTime;
            if (actionType== MotionEvent.ACTION_UP){
                this.upTimes++;
                this.actualTypeIn.add(upperActual);
                if(upperActual==this.target){
                    return true;
                }else
                    return false;
            }
            return false;
        }
        String getTurn(){
            String turnToReturn="turnIndex="+String.valueOf(this.turnIndex)+"\n"+
                    "target="+String.valueOf(this.target)+"\n"+
                    "firstTouch"+String.valueOf(this.firstTouch)+"\n"+
                    "upTimes="+String.valueOf(this.upTimes)+"\n";
            for(int typeIndex=0;typeIndex<this.actualTypeIn.size();typeIndex++){
                turnToReturn+="actualTypeIn "+String.valueOf(typeIndex+1)+"="+this.actualTypeIn.get(typeIndex)+"\n";
            }
            return turnToReturn;
        }
        String getData(){
            String dataToReturn="";
            for (int i=0;i<data.size();i++){
                dataToReturn+=data.get(i).getData();
            }
            return dataToReturn;
        }
    }
    String getTestData(){
        String explain="Data的记录格式为:目标键入字母 实际键入字母 横坐标(相对于键盘) 纵坐标(相对于键盘) 触摸事件类型(按下:0;抬起:1;移动:2) 距离上次触摸事件时间(单位:ms) 目前在播音频数量加上待播音频数量\n";
        String data="MAX_TURN="+String.valueOf(MAX_TURN)+'\n'+
                "screen_width_ratio="+String.valueOf(screen_width_ratio)+'\n'+
                "screen_height_ratio="+String.valueOf(screen_height_ratio)+'\n'+
                "voiceSpeed="+String.valueOf(voiceSpeed)+'\n';

        return explain+data;
    }
    //Fuzzy Input Test Function

    public void testWidgetInit(){
        testTurnChange.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Test begin
                switch (activity_mode){
                    case KEYBOARD_MODE:{//改变测试轮数
                        MAX_TURN=(MAX_TURN+1)%8+1;
                        testTurn.setText(String.valueOf(MAX_TURN));
                        MAX_FUZZY_INPUT_TURN=MAX_TURN*26;
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{//回退一轮

                        if(fuzzyInputTestFigerRecord.size()>fuzzyInputTestTurn)
                            fuzzyInputTestFigerRecord.remove(fuzzyInputTestTurn);//将本轮清空
                        if(fuzzyInputTestTurn>0)
                            fuzzyInputTestTurn--;
                        if(fuzzyInputTestFigerRecord.size()>fuzzyInputTestTurn)
                            fuzzyInputTestFigerRecord.remove(fuzzyInputTestTurn);//将上一轮清空
                        if (fuzzyInputTestTurn < 10) {//10轮熟悉之后加快测试速度
                            playMedia("fuzzyInput", FUZZY_INPUT_SOUND_NEXT);
                        }
                        playMedia("google", fuzzyInputTestList.get(fuzzyInputTestTurn%26));
                        String nextChar = String.valueOf((char) ('A' + fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
                        fuzzyInputTestCharShow.setText(String.valueOf(fuzzyInputTestTurn)+" "+nextChar);
                        progressBar.setProgress(fuzzyInputTestTurn * 100 / MAX_FUZZY_INPUT_TURN);
                        break;
                    }
                }
            }
        });
        fuzzyInputTestBegin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Test begin
                switch (activity_mode){
                    case KEYBOARD_MODE:{
                        beginFuzzyInputTest();
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{
                        restartFuzzyInputTest();
                        break;
                    }
                }
            }
        });
        testMode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                testmode=(testmode+1)%4;
                switch(testmode){
                    case NORMAL:{
                        testMode.setText("NORMAL");
                        break;
                    }
                    case CANCEL:{
                        testMode.setText("CANCEL");
                        break;
                    }
                    case BORDER:{
                        testMode.setText("BORDER");
                        break;
                    }
                    case CANCEL_BORDER:{
                        testMode.setText("CANBOR");
                        break;
                    }
                }
            }
        });
    }
    public void testListInit() {
        for (int i = 0; i < 26; i++) {
            fuzzyInputTestList.add(i % 26);
        }
        Collections.shuffle(fuzzyInputTestList);
    }
    public void beginFuzzyInputTest(){
        activity_mode=FUZZY_INPUT_TEST_MODE;
        fuzzyInputTestCharShow.setVisibility(View.VISIBLE);
        progressBar.setVisibility(View.VISIBLE);
        keyboard.setVisibility(View.INVISIBLE);
        candidatesView.setVisibility(View.GONE);
        readListView.setVisibility(View.GONE);
        text.setText("");
        initModeButton.setText("Back");
        confirmButton.setText("Save");
        fuzzyInputTestBegin.setText("Restart");
        testListInit();
        fuzzyInputTestTurn=0;
        playMedia("fuzzyInput",FUZZY_INPUT_SOUND_BEGIN);
        playMedia("google",fuzzyInputTestList.get(fuzzyInputTestTurn%26));
        String nextChar=String.valueOf((char)('A'+fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
        fuzzyInputTestCharShow.setText(String.valueOf(fuzzyInputTestTurn)+" "+nextChar);
        progressBar.setProgress(fuzzyInputTestTurn*100/MAX_FUZZY_INPUT_TURN);
        ifSave=false;
        ifCalDone=false;
        fuzzyInputTestFigerRecord.clear();
        testTurnChange.setText("BackSpace");
    }
    @RequiresApi(api = Build.VERSION_CODES.N)
    public void saveFuzzyInputTestData(){
        if(ifSave){
            toast("已经存储过一次了");
        }else {
            if (fuzzyInputTestTurn != MAX_FUZZY_INPUT_TURN) {
                toast("请先完成测试");
            } else if (!ifCalDone) {
                toast("还没算完");
            } else {
                if (isExternalStorageWritable()) {
                    //String directory="/storage/emulated/0/Android/data/FuzzyInput/";
                    //File sdCard = Environment.getExternalStorageDirectory();
                    File DirectoryFolder = this.getExternalFilesDir(null);
                    //File DirectoryFolder = new File(fuzzyInputTestStoragePath);
                    //File DirectoryFolder = this.getExternalStoragePublicDirectory();
                    if (!DirectoryFolder.exists()) { //如果该文件夹不存在，则进行创建
                        DirectoryFolder.mkdirs();//创建文件夹
                    }
                    String time=getTime();
                    File fileInDetail = new File(DirectoryFolder, time+ "_详细数据.txt");
                    File fileInRatio = new File(DirectoryFolder,time+"_概率数据.txt");
                    if (!fileInDetail.exists()) {
                        try {
                            fileInDetail.createNewFile();
                            //file is create
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    if (!fileInRatio.exists()) {
                        try {
                            fileInRatio.createNewFile();
                            //file is create
                        } catch (IOException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }
                    try {
                        FileOutputStream fosInDetail = new FileOutputStream(fileInDetail);
                        fosInDetail.write(DataToSave.getBytes());
                        fosInDetail.close();

                        FileOutputStream fosInRatio = new FileOutputStream(fileInRatio);
                        fosInRatio.write(DataToShowAndSave.getBytes());
                        fosInRatio.close();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    toast("存储完毕！");
                    ifSave = true;

                } else {
                    toast("不能存储文件!");
                }
            }
        }
    }

    public void restartFuzzyInputTest(){
        stopVoice();
        if(!ifSave){
            AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("警告");
            dialog.setMessage("测试还未完成，你确定要重新开始吗？");
            dialog.setCancelable(false);
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    fuzzyInputTestTurn=0;
                    playMedia("fuzzyInput",FUZZY_INPUT_SOUND_BEGIN);
                    playMedia("google",fuzzyInputTestList.get(fuzzyInputTestTurn%26));
                    String nextChar=String.valueOf((char)('A'+fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
                    fuzzyInputTestCharShow.setText(nextChar);
                    progressBar.setProgress(fuzzyInputTestTurn*100/MAX_FUZZY_INPUT_TURN);
                    ifSave=false;
                    ifCalDone=false;
                    listView.setVisibility(View.GONE);
                    fuzzyInputTestFigerRecord.clear();
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            dialog.show();
        }else{
            fuzzyInputTestTurn=0;
            playMedia("fuzzyInput",FUZZY_INPUT_SOUND_BEGIN);
            playMedia("google",fuzzyInputTestList.get(fuzzyInputTestTurn%26));
            String nextChar=String.valueOf((char)('A'+fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
            fuzzyInputTestCharShow.setText(nextChar);
            progressBar.setProgress(fuzzyInputTestTurn*100/MAX_FUZZY_INPUT_TURN);
            ifSave=false;
            ifCalDone=false;
            listView.setVisibility(View.GONE);
            fuzzyInputTestFigerRecord.clear();
        }

    }
    public void stopFuzzyInputTest(){
        stopVoice();
        if(!ifSave){
            AlertDialog.Builder dialog=new AlertDialog.Builder(MainActivity.this);
            dialog.setTitle("警告");
            dialog.setMessage("测试还未完成，你确定要返回吗？");
            dialog.setCancelable(false);
            dialog.setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {
                    activity_mode = KEYBOARD_MODE;
                    fuzzyInputTestCharShow.setVisibility(View.GONE);
                    progressBar.setVisibility(View.GONE);
                    listView.setVisibility(View.GONE);
                    keyboard.setVisibility(View.VISIBLE);
                    candidatesView.setVisibility(View.VISIBLE);
                    readListView.setVisibility(View.VISIBLE);
                    fuzzyInputTestBegin.setText("Test Begin");
                    testTurnChange.setText("TESTTURN");
                    refresh();
                    ifSave=false;
                    ifCalDone=false;
                    fuzzyInputTestFigerRecord.clear();
                }
            });
            dialog.setNegativeButton("No", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialogInterface, int i) {

                }
            });
            dialog.show();
        }else {
            activity_mode = KEYBOARD_MODE;
            fuzzyInputTestCharShow.setVisibility(View.GONE);
            progressBar.setVisibility(View.GONE);
            listView.setVisibility(View.GONE);
            keyboard.setVisibility(View.VISIBLE);
            candidatesView.setVisibility(View.VISIBLE);
            readListView.setVisibility(View.VISIBLE);
            fuzzyInputTestBegin.setText("Test Begin");
            refresh();
            fuzzyInputTestFigerRecord.clear();
        }
    }
    int sum(int x[],int length){
        int ans=0;
        for (int i=0;i<length;i++){
            ans+=x[i];
        }
        return ans;
    }
    public void calculateFuzzyInputData(){
        if(fuzzyInputTestTurn!=MAX_FUZZY_INPUT_TURN){
            toast("不应该在这时计算!");
        }
        else{
            int keyNearByData[][]=new int[26][26];
            for (int i=0;i<26;i++){
                for(int j=0;j<26;j++){
                    keyNearByData[i][j]=0;
                }
            }
            DataToSave =getTestData();
            ArrayList<String> listViewData=new ArrayList<String>();
            for(int i=0;i<MAX_FUZZY_INPUT_TURN;i++) {
                Turn turn = fuzzyInputTestFigerRecord.get(i);
                String eachTurn = turn.getTurn();
                keyNearByData[turn.target - 'A'][turn.firstTouch - 'A']++;
                //listViewData.add(eachTurn);
                DataToSave += eachTurn;
                DataToSave += turn.getData();
            }
            float keyNearByRatio[][]=new float[26][26];
            for (int i=0;i<26;i++){
                int totalTimes=sum(keyNearByData[i],26);
                if (totalTimes!=MAX_TURN){
                    String temp="计算"+String.valueOf((char)(i+'A'))+"的键入概率时总次数为"+String.valueOf(totalTimes);
                    listViewData.add(temp);
                    toast(temp);
                }
                for (int j=0;j<26;j++){
                    keyNearByRatio[i][j]=(float)keyNearByData[i][j]/(float)totalTimes;
                    String temp=String.valueOf((char)(i+'A'))+" "+String.valueOf((char)(j+'A'))+" "+String.valueOf(keyNearByRatio[i][j]);
                    DataToShowAndSave+=temp+'\n';
                    if (keyNearByRatio[i][j]!=0)
                        listViewData.add(temp);
                }
            }
            ifCalDone=true;
            ArrayAdapter<String> adapter=new ArrayAdapter<String>(MainActivity.this,android.R.layout.simple_list_item_1,listViewData);
            listView.setVisibility(View.VISIBLE);
            listView.setAdapter(adapter);

        }
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    public String getTime()
    {
        long time=System.currentTimeMillis();//long now = android.os.SystemClock.uptimeMillis();
        SimpleDateFormat format=new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss");
        Date d1=new Date(time);
        return format.format(d1);
    }
    public boolean isExternalStorageWritable() {
        String state = Environment.getExternalStorageState();
        return Environment.MEDIA_MOUNTED.equals(state);
    }
    public void toast(String textForToast){
        Toast mytoast = Toast.makeText(getApplicationContext(),
                textForToast, Toast.LENGTH_SHORT);
        mytoast.setGravity(Gravity.CENTER, 0, 0);
        mytoast.show();
    }
    public void getScreenSizeRatio(){
        DisplayMetrics metrics =new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
        screen_width_ratio = metrics.widthPixels/1440F;
        screen_height_ratio = metrics.heightPixels/2560F;
    }
    // keyboard show
    public class AutoKeyboard extends View{

        Paint backgroundPaint;
        Paint textPaint;

        float screen_width_ratio = 1F;
        float screen_height_ratio = 1F;
        //Fuzzy Input Test Var
        float keyboardHeight=570;
        float keyboardWidth=1438;
        float upThreshold=-190;// 上界
        float downThreshold=760;// 下界
        float minWidth=72;// 最小键宽
        float minHetight=95;//最小键长
        int keyPos[];
        int[] location;
        class KEY{
            char ch;
            float init_x;
            float init_y;
            float curr_x;
            float curr_y;
            float test_x;
            float test_y;
            float init_width;
            float init_height;
            float curr_width;
            float curr_height;
            float test_width;
            float test_height;
            float getDist(float x,float y,int mode){
                // mode==0 init_layout
                // mode==1 current_layout
                if(mode==0){
                    return (init_x-x)*(init_x-x)+(init_y-y)*(init_y-y);
                }
                else{
                    return (curr_x-x)*(curr_x-x)+(curr_y-y)*(curr_y-y);
                }
            }
        }
        KEY keys[];


        void getScreenSizeRatio(){
            DisplayMetrics metrics =new DisplayMetrics();
            getWindowManager().getDefaultDisplay().getRealMetrics(metrics);
            screen_width_ratio = metrics.widthPixels/1440F;
            screen_height_ratio = metrics.heightPixels/2560F;
        }

        public char getKeyByPosition(float x, float y, int mode){
            // mode==0 init_layout
            // mode==1 current_layout
            char key = KEY_NOT_FOUND;
            if(y<upThreshold || y>downThreshold)
                return key;
            float min_dist = Float.MAX_VALUE;
            for (int i = 0; i < 26; ++i){
                float dist_temp=keys[i].getDist(x,y,mode);
                if (dist_temp<min_dist){
                    key=(char)('a'+i);
                    min_dist=dist_temp;
                }
            }
            return key;
        };
        public  boolean tryLayout(char ch,int x,int y){
            ch=Character.toUpperCase(ch);
            int pos = keyPos[ch-'A'];
            float dX=x-keys[pos].init_x;
            float dY=y-keys[pos].init_y;
            if(dY>=0){// 向下平移
                if(keys[19].init_y+dY>downThreshold){
                    return false;
                }
                else{
                    for (int i=0;i<26;i++){
                        keys[i].test_y=keys[i].init_y+dY;
                        keys[i].test_height=keys[i].init_height;
                    }
                }
            }else{// 向上平移
                if(keys[0].init_y+dY<upThreshold){
                    return false;
                }else{
                    for (int i=0;i<26;i++){
                        keys[i].test_y=keys[i].init_y+dY;
                        keys[i].test_height=keys[i].init_height;
                    }
                }
            }

            if(dX>=0) {// 向右平移
                if (pos == 9 || pos == 18 || pos == 25)
                    return false;
            }else {
                if (pos == 0 || pos == 10 || pos == 19)
                    return false;
            }

            if(pos<10 ){// 第一排
                keys[pos].test_x=x;
                keys[pos].test_width=keys[pos].init_width;

                float rightRatio=(keyboardWidth-keys[pos].test_x-keys[pos].test_width/2)/(keyboardWidth-keys[pos].init_x-keys[pos].init_width/2);
                float leftRatio=(keys[pos].test_x-keys[pos].test_width)/(keys[pos].init_x-keys[pos].init_width);

                for(int i=0;i<pos;i++){
                    keys[i].test_x=keys[i].init_x*leftRatio;
                    keys[i].test_width=keys[i].init_width*leftRatio;
                }
                for (int i=pos+1;i<10;i++){
                    keys[i].test_x=keyboardWidth-(keyboardWidth-keys[i].init_x)*rightRatio;
                    keys[i].test_width=keys[i].init_width*rightRatio;
                }

                for (int i=10;i<19;i++){
                    keys[i].test_x=(keys[i-10].test_x+keys[i-9].test_x)/2;
                    keys[i].test_width=keys[i-9].test_x-keys[i-10].test_x;
                }
                for (int i=19;i<26;i++){
                    keys[i].test_x=keys[i-8].test_x;
                    keys[i].test_width=keys[i-8].test_width;
                }

            }else if(pos<19){// 第二排
                keys[pos].test_x=x;
                keys[pos].test_width=keys[pos].init_width;

                float rightRatio=(keyboardWidth-keys[pos].test_x-keys[pos].test_width/2)/(keyboardWidth-keys[pos].init_x-keys[pos].init_width/2);
                float leftRatio=(keys[pos].test_x-keys[pos].test_width)/(keys[pos].init_x-keys[pos].init_width);

                for(int i=10;i<pos;i++){
                    keys[i].test_x=keys[i].init_x*leftRatio;
                    keys[i].test_width=keys[i].init_width*leftRatio;
                }
                for (int i=pos+1;i<19;i++){
                    keys[i].test_x=keyboardWidth-(keyboardWidth-keys[i].init_x)*rightRatio;
                    keys[i].test_width=keys[i].init_width*rightRatio;
                }
                keys[0].test_x=keys[10].test_x/2;
                keys[0].test_width=keys[10].test_x;
                keys[9].test_x=(keyboardWidth-keys[18].test_x)/2;
                keys[9].test_width=keyboardWidth-keys[18].test_x;
                for (int i=1;i<9;i++){
                    keys[i].test_x=(keys[i+9].test_x+keys[i+10].test_x)/2;
                    keys[i].test_width=keys[i+10].test_x-keys[i+9].test_x;
                }
                for (int i=19;i<26;i++){
                    keys[i].test_x=keys[i-8].test_x;
                    keys[i].test_width=keys[i-8].test_width;
                }
            }else{// 第三排
                keys[pos].test_x=x;
                keys[pos].test_width=keys[pos].init_width;

                float rightRatio=(keyboardWidth-keys[pos].test_x-keys[pos].test_width/2)/(keyboardWidth-keys[pos].init_x-keys[pos].init_width/2);
                float leftRatio=(keys[pos].test_x-keys[pos].test_width)/(keys[pos].init_x-keys[pos].init_width);

                for(int i=19;i<pos;i++){
                    keys[i].test_x=keys[i].init_x*leftRatio;
                    keys[i].test_width=keys[i].init_width*leftRatio;
                }
                for (int i=pos+1;i<26;i++){
                    keys[i].test_x=keyboardWidth-(keyboardWidth-keys[i].init_x)*rightRatio;
                    keys[i].test_width=keys[i].init_width*rightRatio;
                }

                for (int i=11;i<18;i++){
                    keys[i].test_x=keys[i+8].test_x;
                    keys[i].test_width=keys[i+8].test_width;
                }
                keys[10].test_width=keys[11].test_x-keys[11].test_width/2;
                keys[10].test_x=keys[10].test_width/2;
                keys[18].test_width=keyboardWidth-keys[17].test_x-keys[17].test_width/2;
                keys[18].test_x=keyboardWidth-keys[18].test_width/2;

                keys[0].test_x=keys[10].test_x/2;
                keys[0].test_width=keys[10].test_x;
                keys[9].test_x=(keyboardWidth-keys[18].test_x)/2;
                keys[9].test_width=keyboardWidth-keys[18].test_x;
                for (int i=1;i<9;i++){
                    keys[i].test_x=(keys[i+9].test_x+keys[i+10].test_x)/2;
                    keys[i].test_width=keys[i+10].test_x-keys[i+9].test_x;
                }
            }


            for (int i=0;i<26;i++){
                if(keys[i].test_width<minWidth){
                    return false;
                }
            }
            for (int i=0;i<26;i++){
                keys[i].curr_x=keys[i].test_x;
                keys[i].curr_y=keys[i].test_y;
                keys[i].curr_height=keys[i].test_height;
                keys[i].curr_width=keys[i].test_width;
            }
            return true;
        }

        public void setLayout(){ // curr_x,curr_y
            invalidate();
        }

        public void resetLayout(){
            keys[0].ch='Q';
            keys[1].ch='W';
            keys[2].ch='E';
            keys[3].ch='R';
            keys[4].ch='T';
            keys[5].ch='Y';
            keys[6].ch='U';
            keys[7].ch='I';
            keys[8].ch='O';
            keys[9].ch='P';
            keys[10].ch='A';
            keys[11].ch='S';
            keys[12].ch='D';
            keys[13].ch='F';
            keys[14].ch='G';
            keys[15].ch='H';
            keys[16].ch='J';
            keys[17].ch='K';
            keys[18].ch='L';
            keys[19].ch='Z';
            keys[20].ch='X';
            keys[21].ch='C';
            keys[22].ch='V';
            keys[23].ch='B';
            keys[24].ch='N';
            keys[25].ch='M';

            for (int i=0;i<10;i++){
                keys[i].init_x=keyboardWidth*(2*i+1)/20;
                keys[i].init_y=keyboardHeight/6;
            }
            for (int i=10;i<19;i++){
                keys[i].init_x=(keys[i-10].init_x+keys[i-9].init_x)/2;
                keys[i].init_y=keyboardHeight/2;
            }
            for (int i=19;i<26;i++){
                keys[i].init_x=keys[i-8].init_x;
                keys[i].init_y=keyboardHeight*5/6;
            }

            for (int i=0;i<26;i++) {
                keys[i].init_height=keyboardHeight/3;
                keys[i].init_width=keyboardWidth/10;
                keys[i].curr_width=keys[i].init_width;
                keys[i].curr_height=keys[i].init_height;
                keys[i].curr_x = keys[i].init_x;
                keys[i].curr_y = keys[i].init_y;
            }
        }
        public AutoKeyboard(Context context, ImageView keyboard){
            super(context);
            backgroundPaint=new Paint();
            textPaint=new Paint();
            backgroundPaint.setColor(Color.rgb(230,255,255));
            backgroundPaint.setStrokeJoin(Paint.Join.ROUND);
            backgroundPaint.setStrokeCap(Paint.Cap.ROUND);
            backgroundPaint.setStrokeWidth(3);

            textPaint.setColor(Color.BLACK);
            textPaint.setStrokeJoin(Paint.Join.ROUND);
            textPaint.setStrokeCap(Paint.Cap.ROUND);
            textPaint.setStrokeWidth(4);

            keyboard.getLocationOnScreen(location);
            getScreenSizeRatio();
            keyboardHeight=keyboardHeight*screen_height_ratio;
            keyboardWidth=keyboardWidth*screen_width_ratio;
            upThreshold=screen_height_ratio*upThreshold;
            downThreshold=screen_width_ratio*downThreshold;
            minWidth=screen_width_ratio*minWidth;
            minHetight=screen_height_ratio*minHetight;
            this.keys=new KEY[26];
            this.location=new int[2];
            this.keyPos=new int[]{10,23,21,12,2,13,14,15,7,16,17,18,25,24,8,9,0,3,11,4,6,22,1,20,5,19};// A-Z 对应 Q-M
            resetLayout();
            setLayout();
        }
        @Override
        protected void onDraw(Canvas canvas) {
            float left=location[0]+keys[0].curr_x-keys[0].curr_width/2;
            float top=location[1]+keys[0].curr_y-keys[0].curr_height/2;
            float right=location[0]+keys[25].curr_x+keys[25].curr_width/2;
            float bottom=location[1]+keys[25].curr_y+keys[25].curr_height/2;

            RectF rect = new RectF(left, top, right, bottom);
            canvas.drawRect(rect, backgroundPaint);
            for (int i=0;i<26;i++){
                canvas.drawText(String.valueOf(keys[i].ch),keys[i].curr_x+location[0],keys[i].curr_y+location[1],textPaint);
            }
        }
    }
    // init the coordinates of the key a-z on phone
    public void initKeyPosition(){
        key_left['q' - 'a'] = (int)(15F*screen_width_ratio);
        key_right['q' - 'a'] = (int)(137F*screen_width_ratio);
        key_bottom['q' - 'a'] = (int)(167F*screen_width_ratio);
        key_top['q' - 'a'] = (int)(0F*screen_width_ratio);
        for (int i = 1; i < keys[0].length(); ++i){
            key_left[keys[0].charAt(i) - 'a'] = key_left[keys[0].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_right[keys[0].charAt(i) - 'a'] = key_right[keys[0].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_top[keys[0].charAt(i) - 'a'] = key_top[keys[0].charAt(i - 1) - 'a'];
            key_bottom[keys[0].charAt(i) - 'a'] = key_bottom[keys[0].charAt(i - 1) - 'a'];
        }

        key_left['a' - 'a'] = (int)(142F*screen_width_ratio);
        key_right['a' - 'a'] = (int)(209F*screen_width_ratio);
        key_bottom['a' - 'a'] = (int)(354F*screen_width_ratio);
        key_top['a' - 'a'] = (int)(187F*screen_width_ratio);
        for (int i = 1; i < keys[1].length(); ++i){
            key_left[keys[1].charAt(i) - 'a'] = key_left[keys[1].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_right[keys[1].charAt(i) - 'a'] = key_right[keys[1].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_top[keys[1].charAt(i) - 'a'] = key_top[keys[1].charAt(i - 1) - 'a'];
            key_bottom[keys[1].charAt(i) - 'a'] = key_bottom[keys[1].charAt(i - 1) - 'a'];
        }

        key_left['z' - 'a'] = (int)(230F*screen_width_ratio);
        key_right['z' - 'a'] = (int)(352F*screen_width_ratio);
        key_bottom['z' - 'a'] = (int)(541F*screen_width_ratio);
        key_top['z' - 'a'] = (int)(374F*screen_width_ratio);
        for (int i = 1; i < keys[2].length(); ++i){
            key_left[keys[2].charAt(i) - 'a'] = key_left[keys[2].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_right[keys[2].charAt(i) - 'a'] = key_right[keys[2].charAt(i - 1) - 'a'] + (int)(142F*screen_width_ratio);
            key_top[keys[2].charAt(i) - 'a'] = key_top[keys[2].charAt(i - 1) - 'a'];
            key_bottom[keys[2].charAt(i) - 'a'] = key_bottom[keys[2].charAt(i - 1) - 'a'];
        }
    }

    //redraw the views
    public void refresh(){
        text.setText(currentWord + "\n" + currentWord2 + "\n" + currentBaseline);
        String str = "";
        for (int i = 0; i < candidates.size(); ++i)
            str += candidates.get(i).text + "\n";
        candidatesView.setText(str);
        readListView.setText(readList);
        if (initMode == INIT_MODE_ABSOLUTE)
            initModeButton.setText("absolute");
        else if(initMode == INIT_MODE_RELATIVE)
            initModeButton.setText("relative");
        else
            initModeButton.setText("nothing");
    }

    final int MAX_CANDIDATE = 5;
    public ArrayList<Word> candidates = new ArrayList<Word>();

    //predict the candidates according the currentWord and currentWord2 and refresh
    public void predict(String currentWord, String currentWord2){
        candidates.clear();
        for (int i = 0; i < dict.size(); ++i){
            if (candidates.size() >= MAX_CANDIDATE)
                break;
            Word candidate = dict.get(i);
            if (candidate.text.length() != currentWord.length())
                continue;

            boolean flag = true;
            for (int j = 0; j < currentWord.length(); ++j)
                if (candidate.text.charAt(j) != currentWord.charAt(j) && candidate.text.charAt(j) != currentWord2.charAt(j)){
                    flag = false;
                    break;
                }
            if (flag)
                candidates.add(candidate);
        }
        refresh();
    }

    //todo reconstruct
    public void stopVoice(){
        if (current != null) {
            current.stop();
            current.reset();
            current.release();
            current = null;
        }
        myPlayList.clear();
    }

    public void stopInput(){
        seq.clear();
        stopVoice();
    }

    public void finishWord(){
        currentWord = "";
        currentWord2 = "";
        currentBaseline = "";
        readList = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    //new touch point in keyboard area
    public void newPoint(int x, int y){
        if (seq.size() == 0){//first touch
            if (initMode == INIT_MODE_ABSOLUTE) {
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                deltaX = (key_left[ch - 'a'] + key_right[ch - 'a']) / 2 - x; //move to the centre of the key
                deltaY = (key_top[ch - 'a'] + key_bottom[ch - 'a']) / 2 - y;
                addToSeq(getKeyByPosition(x, y), true,true);
            }
            else if(initMode == INIT_MODE_RELATIVE){
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x, y);
                char best = addToSeq(ch, false,true);
                deltaX = (key_left[best - 'a'] + key_right[best - 'a']) / 2 - x; //move to the centre of the most possible key
                deltaY = (key_top[best - 'a'] + key_bottom[best - 'a']) / 2 - y;
                addToSeq(best, true,true);
            }else{
                deltaX = 0;
                deltaY = 0;
                char ch = getKeyByPosition(x,y);
                addToSeq(ch,true,false);
            }
        }
        else{
            addToSeq(getKeyByPosition(x, y), true,true);
        }
    }

    //init keysNearby and keysNearbyProb
    public void initKeyboard(){
        int delta[][] = new int [][]{{-1, -1, 0, 0, 0, 1, 1},{0, 1, -1, 0, 1, -1, 0}};
        double prob[] = new double[]{0.042, 0.042, 0.192, 0.376, 0.192, 0.044, 0.023};
        for (int  i= 0; i < 3; ++i)
            for (int j = 0; j < keys[i].length(); ++j){
                char ch = keys[i].charAt(j);
                keysNearby[ch - 'a'] = "";
                for (int k = 0; k < 7; ++k){
                    int _i = i + delta[0][k], _j = j + delta[1][k];
                    if (_i >= 0 && _i < 3 && _j >= 0 && _j < keys[_i].length()) {
                        keysNearby[ch - 'a'] += keys[_i].charAt(_j);
                        keysNearbyProb[ch - 'a'][keysNearby[ch - 'a'].length() - 1] = prob[k];
                    }
                }
            }
    }

    final int DICT_SIZE = 10000;
    //read dict from file
    public void initDict(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict)));
        String line;
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict.add(new Word(ss[0], Double.valueOf(ss[1])));
                if (lineNo == DICT_SIZE)
                    break;
            }
            reader.close();
            Log.i("init", "read dictionary finished " + dict.size());
        } catch (Exception e){
            Log.i("init", "read dictionary failed");
        }
    }

    //todo reconstruct
    public void initVoice() {

        voice.put("ios11_50", new int[26]);
        voice.put("ios11_60", new int[26]);
        voice.put("ios11_70", new int[26]);
        voice.put("ios11_80", new int[26]);
        voice.put("ios11_90", new int[26]);
        voice.put("ios11_100", new int[26]);
        voice.put("ios11da", new int[1]);
        voice.put("delete", new int[1]);
        voice.put("fuzzyInput",new int[3]);
        voice.put("google",new int[26]);

        voice.get("fuzzyInput")[0]=R.raw.fuzzy_input_test_begin;
        voice.get("fuzzyInput")[1]=R.raw.fuzzy_input_test_next;
        voice.get("fuzzyInput")[2]=R.raw.fuzzy_input_test_end;

        voice.get("google")[ 0] = R.raw.google_a;
        voice.get("google")[ 1] = R.raw.google_b;
        voice.get("google")[ 2] = R.raw.google_c;
        voice.get("google")[ 3] = R.raw.google_d;
        voice.get("google")[ 4] = R.raw.google_e;
        voice.get("google")[ 5] = R.raw.google_f;
        voice.get("google")[ 6] = R.raw.google_g;
        voice.get("google")[ 7] = R.raw.google_h;
        voice.get("google")[ 8] = R.raw.google_i;
        voice.get("google")[ 9] = R.raw.google_j;
        voice.get("google")[10] = R.raw.google_k;
        voice.get("google")[11] = R.raw.google_l;
        voice.get("google")[12] = R.raw.google_m;
        voice.get("google")[13] = R.raw.google_n;
        voice.get("google")[14] = R.raw.google_o;
        voice.get("google")[15] = R.raw.google_p;
        voice.get("google")[16] = R.raw.google_q;
        voice.get("google")[17] = R.raw.google_r;
        voice.get("google")[18] = R.raw.google_s;
        voice.get("google")[19] = R.raw.google_t;
        voice.get("google")[20] = R.raw.google_u;
        voice.get("google")[21] = R.raw.google_v;
        voice.get("google")[22] = R.raw.google_w;
        voice.get("google")[23] = R.raw.google_x;
        voice.get("google")[24] = R.raw.google_y;
        voice.get("google")[25] = R.raw.google_z;

        voice.get("ios11_50")[ 0] = R.raw.ios11_50_a;
        voice.get("ios11_50")[ 1] = R.raw.ios11_50_b;
        voice.get("ios11_50")[ 2] = R.raw.ios11_50_c;
        voice.get("ios11_50")[ 3] = R.raw.ios11_50_d;
        voice.get("ios11_50")[ 4] = R.raw.ios11_50_e;
        voice.get("ios11_50")[ 5] = R.raw.ios11_50_f;
        voice.get("ios11_50")[ 6] = R.raw.ios11_50_g;
        voice.get("ios11_50")[ 7] = R.raw.ios11_50_h;
        voice.get("ios11_50")[ 8] = R.raw.ios11_50_i;
        voice.get("ios11_50")[ 9] = R.raw.ios11_50_j;
        voice.get("ios11_50")[10] = R.raw.ios11_50_k;
        voice.get("ios11_50")[11] = R.raw.ios11_50_l;
        voice.get("ios11_50")[12] = R.raw.ios11_50_m;
        voice.get("ios11_50")[13] = R.raw.ios11_50_n;
        voice.get("ios11_50")[14] = R.raw.ios11_50_o;
        voice.get("ios11_50")[15] = R.raw.ios11_50_p;
        voice.get("ios11_50")[16] = R.raw.ios11_50_q;
        voice.get("ios11_50")[17] = R.raw.ios11_50_r;
        voice.get("ios11_50")[18] = R.raw.ios11_50_s;
        voice.get("ios11_50")[19] = R.raw.ios11_50_t;
        voice.get("ios11_50")[20] = R.raw.ios11_50_u;
        voice.get("ios11_50")[21] = R.raw.ios11_50_v;
        voice.get("ios11_50")[22] = R.raw.ios11_50_w;
        voice.get("ios11_50")[23] = R.raw.ios11_50_x;
        voice.get("ios11_50")[24] = R.raw.ios11_50_y;
        voice.get("ios11_50")[25] = R.raw.ios11_50_z;

        voice.get("ios11_60")[ 0] = R.raw.ios11_60_a;
        voice.get("ios11_60")[ 1] = R.raw.ios11_60_b;
        voice.get("ios11_60")[ 2] = R.raw.ios11_60_c;
        voice.get("ios11_60")[ 3] = R.raw.ios11_60_d;
        voice.get("ios11_60")[ 4] = R.raw.ios11_60_e;
        voice.get("ios11_60")[ 5] = R.raw.ios11_60_f;
        voice.get("ios11_60")[ 6] = R.raw.ios11_60_g;
        voice.get("ios11_60")[ 7] = R.raw.ios11_60_h;
        voice.get("ios11_60")[ 8] = R.raw.ios11_60_i;
        voice.get("ios11_60")[ 9] = R.raw.ios11_60_j;
        voice.get("ios11_60")[10] = R.raw.ios11_60_k;
        voice.get("ios11_60")[11] = R.raw.ios11_60_l;
        voice.get("ios11_60")[12] = R.raw.ios11_60_m;
        voice.get("ios11_60")[13] = R.raw.ios11_60_n;
        voice.get("ios11_60")[14] = R.raw.ios11_60_o;
        voice.get("ios11_60")[15] = R.raw.ios11_60_p;
        voice.get("ios11_60")[16] = R.raw.ios11_60_q;
        voice.get("ios11_60")[17] = R.raw.ios11_60_r;
        voice.get("ios11_60")[18] = R.raw.ios11_60_s;
        voice.get("ios11_60")[19] = R.raw.ios11_60_t;
        voice.get("ios11_60")[20] = R.raw.ios11_60_u;
        voice.get("ios11_60")[21] = R.raw.ios11_60_v;
        voice.get("ios11_60")[22] = R.raw.ios11_60_w;
        voice.get("ios11_60")[23] = R.raw.ios11_60_x;
        voice.get("ios11_60")[24] = R.raw.ios11_60_y;
        voice.get("ios11_60")[25] = R.raw.ios11_60_z;

        voice.get("ios11_70")[ 0] = R.raw.ios11_70_a;
        voice.get("ios11_70")[ 1] = R.raw.ios11_70_b;
        voice.get("ios11_70")[ 2] = R.raw.ios11_70_c;
        voice.get("ios11_70")[ 3] = R.raw.ios11_70_d;
        voice.get("ios11_70")[ 4] = R.raw.ios11_70_e;
        voice.get("ios11_70")[ 5] = R.raw.ios11_70_f;
        voice.get("ios11_70")[ 6] = R.raw.ios11_70_g;
        voice.get("ios11_70")[ 7] = R.raw.ios11_70_h;
        voice.get("ios11_70")[ 8] = R.raw.ios11_70_i;
        voice.get("ios11_70")[ 9] = R.raw.ios11_70_j;
        voice.get("ios11_70")[10] = R.raw.ios11_70_k;
        voice.get("ios11_70")[11] = R.raw.ios11_70_l;
        voice.get("ios11_70")[12] = R.raw.ios11_70_m;
        voice.get("ios11_70")[13] = R.raw.ios11_70_n;
        voice.get("ios11_70")[14] = R.raw.ios11_70_o;
        voice.get("ios11_70")[15] = R.raw.ios11_70_p;
        voice.get("ios11_70")[16] = R.raw.ios11_70_q;
        voice.get("ios11_70")[17] = R.raw.ios11_70_r;
        voice.get("ios11_70")[18] = R.raw.ios11_70_s;
        voice.get("ios11_70")[19] = R.raw.ios11_70_t;
        voice.get("ios11_70")[20] = R.raw.ios11_70_u;
        voice.get("ios11_70")[21] = R.raw.ios11_70_v;
        voice.get("ios11_70")[22] = R.raw.ios11_70_w;
        voice.get("ios11_70")[23] = R.raw.ios11_70_x;
        voice.get("ios11_70")[24] = R.raw.ios11_70_y;
        voice.get("ios11_70")[25] = R.raw.ios11_70_z;

        voice.get("ios11_80")[ 0] = R.raw.ios11_80_a;
        voice.get("ios11_80")[ 1] = R.raw.ios11_80_b;
        voice.get("ios11_80")[ 2] = R.raw.ios11_80_c;
        voice.get("ios11_80")[ 3] = R.raw.ios11_80_d;
        voice.get("ios11_80")[ 4] = R.raw.ios11_80_e;
        voice.get("ios11_80")[ 5] = R.raw.ios11_80_f;
        voice.get("ios11_80")[ 6] = R.raw.ios11_80_g;
        voice.get("ios11_80")[ 7] = R.raw.ios11_80_h;
        voice.get("ios11_80")[ 8] = R.raw.ios11_80_i;
        voice.get("ios11_80")[ 9] = R.raw.ios11_80_j;
        voice.get("ios11_80")[10] = R.raw.ios11_80_k;
        voice.get("ios11_80")[11] = R.raw.ios11_80_l;
        voice.get("ios11_80")[12] = R.raw.ios11_80_m;
        voice.get("ios11_80")[13] = R.raw.ios11_80_n;
        voice.get("ios11_80")[14] = R.raw.ios11_80_o;
        voice.get("ios11_80")[15] = R.raw.ios11_80_p;
        voice.get("ios11_80")[16] = R.raw.ios11_80_q;
        voice.get("ios11_80")[17] = R.raw.ios11_80_r;
        voice.get("ios11_80")[18] = R.raw.ios11_80_s;
        voice.get("ios11_80")[19] = R.raw.ios11_80_t;
        voice.get("ios11_80")[20] = R.raw.ios11_80_u;
        voice.get("ios11_80")[21] = R.raw.ios11_80_v;
        voice.get("ios11_80")[22] = R.raw.ios11_80_w;
        voice.get("ios11_80")[23] = R.raw.ios11_80_x;
        voice.get("ios11_80")[24] = R.raw.ios11_80_y;
        voice.get("ios11_80")[25] = R.raw.ios11_80_z;

        voice.get("ios11_90")[ 0] = R.raw.ios11_90_a;
        voice.get("ios11_90")[ 1] = R.raw.ios11_90_b;
        voice.get("ios11_90")[ 2] = R.raw.ios11_90_c;
        voice.get("ios11_90")[ 3] = R.raw.ios11_90_d;
        voice.get("ios11_90")[ 4] = R.raw.ios11_90_e;
        voice.get("ios11_90")[ 5] = R.raw.ios11_90_f;
        voice.get("ios11_90")[ 6] = R.raw.ios11_90_g;
        voice.get("ios11_90")[ 7] = R.raw.ios11_90_h;
        voice.get("ios11_90")[ 8] = R.raw.ios11_90_i;
        voice.get("ios11_90")[ 9] = R.raw.ios11_90_j;
        voice.get("ios11_90")[10] = R.raw.ios11_90_k;
        voice.get("ios11_90")[11] = R.raw.ios11_90_l;
        voice.get("ios11_90")[12] = R.raw.ios11_90_m;
        voice.get("ios11_90")[13] = R.raw.ios11_90_n;
        voice.get("ios11_90")[14] = R.raw.ios11_90_o;
        voice.get("ios11_90")[15] = R.raw.ios11_90_p;
        voice.get("ios11_90")[16] = R.raw.ios11_90_q;
        voice.get("ios11_90")[17] = R.raw.ios11_90_r;
        voice.get("ios11_90")[18] = R.raw.ios11_90_s;
        voice.get("ios11_90")[19] = R.raw.ios11_90_t;
        voice.get("ios11_90")[20] = R.raw.ios11_90_u;
        voice.get("ios11_90")[21] = R.raw.ios11_90_v;
        voice.get("ios11_90")[22] = R.raw.ios11_90_w;
        voice.get("ios11_90")[23] = R.raw.ios11_90_x;
        voice.get("ios11_90")[24] = R.raw.ios11_90_y;
        voice.get("ios11_90")[25] = R.raw.ios11_90_z;

        voice.get("ios11_100")[ 0] = R.raw.ios11_100_a;
        voice.get("ios11_100")[ 1] = R.raw.ios11_100_b;
        voice.get("ios11_100")[ 2] = R.raw.ios11_100_c;
        voice.get("ios11_100")[ 3] = R.raw.ios11_100_d;
        voice.get("ios11_100")[ 4] = R.raw.ios11_100_e;
        voice.get("ios11_100")[ 5] = R.raw.ios11_100_f;
        voice.get("ios11_100")[ 6] = R.raw.ios11_100_g;
        voice.get("ios11_100")[ 7] = R.raw.ios11_100_h;
        voice.get("ios11_100")[ 8] = R.raw.ios11_100_i;
        voice.get("ios11_100")[ 9] = R.raw.ios11_100_j;
        voice.get("ios11_100")[10] = R.raw.ios11_100_k;
        voice.get("ios11_100")[11] = R.raw.ios11_100_l;
        voice.get("ios11_100")[12] = R.raw.ios11_100_m;
        voice.get("ios11_100")[13] = R.raw.ios11_100_n;
        voice.get("ios11_100")[14] = R.raw.ios11_100_o;
        voice.get("ios11_100")[15] = R.raw.ios11_100_p;
        voice.get("ios11_100")[16] = R.raw.ios11_100_q;
        voice.get("ios11_100")[17] = R.raw.ios11_100_r;
        voice.get("ios11_100")[18] = R.raw.ios11_100_s;
        voice.get("ios11_100")[19] = R.raw.ios11_100_t;
        voice.get("ios11_100")[20] = R.raw.ios11_100_u;
        voice.get("ios11_100")[21] = R.raw.ios11_100_v;
        voice.get("ios11_100")[22] = R.raw.ios11_100_w;
        voice.get("ios11_100")[23] = R.raw.ios11_100_x;
        voice.get("ios11_100")[24] = R.raw.ios11_100_y;
        voice.get("ios11_100")[25] = R.raw.ios11_100_z;

        voice.get("ios11da")[0] = R.raw.ios11_da;

        voice.get("delete")[0] = R.raw.delete;
    }

    public void initButtons(){
        confirmButton.setOnClickListener(new View.OnClickListener() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void onClick(View v) {
                switch(activity_mode) {
                    case KEYBOARD_MODE:{
                        stopInput();
                        finishWord();
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{
                        saveFuzzyInputTestData();
                        break;
                    }
                    default:
                }
            }
        });

        initModeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                switch(activity_mode){
                    case KEYBOARD_MODE:{
                        if (initMode == INIT_MODE_ABSOLUTE)
                            initMode = INIT_MODE_RELATIVE;
                        else if(initMode == INIT_MODE_RELATIVE)
                            initMode = INIT_MODE_NOTHING;
                        else
                            initMode = INIT_MODE_ABSOLUTE;
                        refresh();
                        break;
                    }
                    case FUZZY_INPUT_TEST_MODE:{
                        stopFuzzyInputTest();
                        break;
                    }
                    default:
                }

            }
        });

        speedControl.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                voiceSpeed=(voiceSpeed+10)%60+50;
                voiceSpeedText.setText(voiceSpeed+"");
            }
        });
    }

    public void playFirstVoice(){
        if (current == null && !myPlayList.isEmpty()){
            current = MediaPlayer.create(this, myPlayList.get(0));
            myPlayList.remove(0);
            current.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.release();
                    current = null;
                    playFirstVoice();
                }
            });
            current.start();
        }
    }

    //todo reconstruct
    public void playMedia(String tag, int index){
        myPlayList.add(voice.get(tag)[index]);
        playFirstVoice();
    }

    //if write=false, just return the most possible key
    public char addToSeq(char ch, boolean write, boolean predictMode){
        if (ch != KEY_NOT_FOUND){
            if (seq.size() == 0 || seq.get(seq.size() - 1) != ch) {
                //make a vibrate
                Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                long[] pattern = {0, 30};
                vibrator.vibrate(pattern, -1);
                ArrayList<Word> letters = new ArrayList<Word>();
                if(predictMode){
                    for (int i = 0; i < keysNearby[ch - 'a'].length(); ++i)
                        letters.add(new Word(keysNearby[ch - 'a'].charAt(i) + "", 0));
                    for (int i = 0; i < dict.size(); ++i)
                        if (dict.get(i).text.length() >= currentWord.length() + 1){
                            boolean flag = true;
                            Word word = dict.get(i);
                            for (int j = 0; j < currentWord.length(); ++j)
                                if (word.text.charAt(j) != currentWord.charAt(j) && word.text.charAt(j) != currentWord2.charAt(j)){
                                    flag = false;
                                    break;
                                }
                            if (flag){
                                for (int j = 0; j < letters.size(); ++j)
                                    if (letters.get(j).text.charAt(0) == word.text.charAt(currentWord.length()))
                                        letters.get(j).freq += word.freq;
                            }
                        }
                    for (int i = 0; i < keysNearby[ch - 'a'].length(); ++i)
                        letters.get(i).freq *= keysNearbyProb[ch - 'a'][i];
                    Collections.sort(letters);
                    if (!write) {
                        if (letters.get(0).freq > 0)
                            return letters.get(0).text.charAt(0);
                        else
                            return ch;
                    }
                }

                //write=true
                seq.add(ch);
                Log.i("seq", ch + "");
                stopVoice();
                readList = "";
                nowCh = '*';
                nowCh2 = '*';

                playMedia("ios11da", 0);
                if (predictMode){
                    if (seq.size() == 1){
                        //prob top 2
                        if (letters.get(0).freq > 0) {
                            nowCh = letters.get(0).text.charAt(0);
                            playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                            readList += nowCh;
                            if (letters.get(1).freq * 10 > letters.get(0).freq){
                                nowCh2 = letters.get(1).text.charAt(0);
                                playMedia("ios11_"+voiceSpeed, nowCh2 - 'a');
                                readList += nowCh2;
                            }
                        }
                        else {
                            //current key
                            nowCh = ch;
                            playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                            readList += nowCh;
                        }
                    }
                    else{
                        //current key
                        nowCh = ch;
                        playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                        readList += nowCh;
                    }
                }else{
                    nowCh = ch;
                    playMedia("ios11_"+voiceSpeed, nowCh - 'a');
                    readList += nowCh;
                }

                refresh();
            }
        }
        return 'a';
    }
    //get key with translation
    char getKeyByPosition(int x, int y){
        x += deltaX;
        y += deltaY;
        char key = KEY_NOT_FOUND;
        int min_dist = Integer.MAX_VALUE;
        for (int i = 0; i < 26; ++i){
            int _x = (key_left[i] + key_right[i]) / 2;
            int _y = (key_top[i] + key_bottom[i]) / 2;
            if ((x - _x) * (x - _x) + (y - _y) * (y - _y) < min_dist){
                key = (char)('a' + i);
                min_dist = (x - _x) * (x - _x) + (y - _y) * (y - _y);
            }
        }
        return key;
    }

    //get key without translation
    char getKeyBaseLine(int x, int y){
        char key = KEY_NOT_FOUND;
        if(y<0 || y>screen_height_ratio*keyboardHeight)
            return key;
        int min_dist = Integer.MAX_VALUE;
        for (int i = 0; i < 26; ++i){
            int _x = (key_left[i] + key_right[i]) / 2;
            int _y = (key_top[i] + key_bottom[i]) / 2;
            if ((x - _x) * (x - _x) + (y - _y) * (y - _y) < min_dist){
                key = (char)('a' + i);
                min_dist = (x - _x) * (x - _x) + (y - _y) * (y - _y);
            }
        }
        return key;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.activity_main_relative);
        keyboard = (ImageView)findViewById(R.id.keyboard);
        //autoKeyboard=new AutoKeyboard(this,keyboard);
        //setContentView(autoKeyboard);

        text = (TextView)findViewById(R.id.text);
        candidatesView = (TextView)findViewById(R.id.candidates);
        confirmButton = (Button)findViewById(R.id.confirm_button);
        readListView = (TextView)findViewById(R.id.readList);
        initModeButton = (Button)findViewById(R.id.init_mode_button);
        speedControl = (Button)findViewById(R.id.speedControl);
        testMode = (Button)findViewById(R.id.testMode);
        voiceSpeedText = (TextView)findViewById(R.id.voice_speed_text);
        fuzzyInputTestBegin=(Button)findViewById(R.id.fuzzyInputTestBegin);
        fuzzyInputTestCharShow=(TextView)findViewById(R.id.fuzzyInputTestCharShow);
        fuzzyInputTestCharShow.setVisibility(View.GONE);
        progressBar=(ProgressBar)findViewById(R.id.progress_bar);
        progressBar.setVisibility(View.GONE);
        listView=(ListView)findViewById(R.id.list_view);
        listView.setVisibility(View.GONE);
        testTurnChange=(Button)findViewById(R.id.testTurnChange);
        testTurn=(TextView)findViewById(R.id.testTurn);
        getScreenSizeRatio();
        initKeyPosition();
        initDict();
        initVoice();
        initButtons();
        initKeyboard();
        testWidgetInit();


        //left 0 right 1440 top 1554 bottom 2320
    }

    @Override
    public void onDestroy(){
        if (current != null){
            current.release();
            current = null;
        }
        super.onDestroy();
    }
    public void deleteLastChar() {
        if (currentWord.length() > 0) {
            currentWord = currentWord.substring(0, currentWord.length() - 1);
            currentWord2 = currentWord2.substring(0, currentWord2.length() - 1);
            currentBaseline = currentBaseline.substring(0, currentBaseline.length() - 1);
        }
        readList = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    public void deleteAllChar(){
        currentWord = "";
        currentWord2 = "";
        currentBaseline = "";
        readList = "";
        predict(currentWord, currentWord2);
        refresh();
    }

    int downX, downY;
    long downTime;
    final long STAY_TIME = 400;
    final int SLIP_DIST = 90;
    char lastChar='A';
    boolean isOverScreen=false;
    public boolean onTouchEvent(MotionEvent event){
        if (event.getPointerCount() == 1 && event.getY() >= (int)(1500F*screen_height_ratio)) {//in the keyboard area
            int x = (int) event.getX();
            int y = (int) event.getY();
            int[] location = new int[2];
            keyboard.getLocationOnScreen(location);
            switch(activity_mode){
                case KEYBOARD_MODE: {
                    switch (event.getActionMasked()) {
                        case MotionEvent.ACTION_DOWN:
                        case MotionEvent.ACTION_POINTER_DOWN:
                            downX = x;
                            downY = y;
                            downTime = System.currentTimeMillis();
                            newPoint(x, y - location[1]);
                            //action down
                            break;

                        case MotionEvent.ACTION_MOVE:
                            newPoint(x, y - location[1]);
                            //action move
                            break;

                        case MotionEvent.ACTION_UP:
                        case MotionEvent.ACTION_POINTER_UP:
                            stopInput();
                            if (x < downX - SLIP_DIST && System.currentTimeMillis() < downTime + STAY_TIME) {
                                deleteLastChar();
                                playMedia("delete", 0);
                            } else if (x > downX + SLIP_DIST && System.currentTimeMillis() < downTime + STAY_TIME) {
                                deleteAllChar();
                                playMedia("delete", 0);
                            } else {
                                currentWord += nowCh;
                                currentWord2 += nowCh2;
                                currentBaseline += getKeyBaseLine(x, y - location[1]);
                                predict(currentWord, currentWord2);
                                refresh();
                            }
                            break;
                    }
                    break;
                }
                    case FUZZY_INPUT_TEST_MODE: {
                        y=y-location[1];
                        if (fuzzyInputTestTurn < MAX_FUZZY_INPUT_TURN && y>=0 && y<=keyboardHeight*screen_height_ratio && !isOverScreen) {
                            switch (event.getActionMasked()) {
                                case MotionEvent.ACTION_DOWN: {
                                    if(fuzzyInputTestTurn>=fuzzyInputTestFigerRecord.size()){
                                        fuzzyInputTestFigerRecord.add(new Turn(fuzzyInputTestTurn,event.getDownTime()));
                                    }
                                    char ch = getKeyBaseLine(x, y);
                                    fuzzyInputTestFigerRecord.get(fuzzyInputTestTurn).addData(ch,x,y,MotionEvent.ACTION_DOWN,event.getEventTime());
                                    stopVoice();
                                    playMedia("ios11_"+voiceSpeed, ch - 'a');
                                    lastChar=ch;
                                }
                                case MotionEvent.ACTION_MOVE: {
                                    char ch = getKeyBaseLine(x, y);
                                    fuzzyInputTestFigerRecord.get(fuzzyInputTestTurn).addData(ch,x,y,MotionEvent.ACTION_MOVE,event.getEventTime());
                                    if(ch!=lastChar){
                                        stopVoice();
                                        playMedia("ios11da", 0);
                                        Vibrator vibrator =  (Vibrator)getApplicationContext().getSystemService(Context.VIBRATOR_SERVICE);
                                        long[] pattern = {0, 30};
                                        vibrator.vibrate(pattern, -1);
                                        playMedia("ios11_"+voiceSpeed, ch - 'a');
                                    }
                                    lastChar=ch;
                                    break;
                                }
                                case MotionEvent.ACTION_UP: {
                                    char ch = getKeyBaseLine(x, y);
                                    boolean isDone=fuzzyInputTestFigerRecord.get(fuzzyInputTestTurn).addData(ch,x,y,MotionEvent.ACTION_UP,event.getEventTime());
                                    stopVoice();
                                    if(isDone){
                                        fuzzyInputTestTurn++;
                                        if (fuzzyInputTestTurn >= MAX_FUZZY_INPUT_TURN) {
                                            fuzzyInputTestCharShow.setText("DONE!");
                                            playMedia("fuzzyInput", FUZZY_INPUT_SOUND_END);
                                            calculateFuzzyInputData();
                                            break;
                                        }
                                    }
                                    if (fuzzyInputTestTurn < 10) {//10轮熟悉之后加快测试速度
                                        playMedia("fuzzyInput", FUZZY_INPUT_SOUND_NEXT);
                                    }
                                    playMedia("google", fuzzyInputTestList.get(fuzzyInputTestTurn%26));
                                    String nextChar = String.valueOf((char) ('A' + fuzzyInputTestList.get(fuzzyInputTestTurn%26)));
                                    fuzzyInputTestCharShow.setText(String.valueOf(fuzzyInputTestTurn)+" "+nextChar);
                                    progressBar.setProgress(fuzzyInputTestTurn * 100 / MAX_FUZZY_INPUT_TURN);
                                    break;
                                }
                                default:
                            }
                        }
                        else if(y<0 || isOverScreen){
                            switch (event.getActionMasked()) {
                                case MotionEvent.ACTION_DOWN: {
                                    isOverScreen=true;
                                    break;
                                }
                                case MotionEvent.ACTION_UP:{
                                    isOverScreen=false;
                                    break;
                                }
                                default:
                            }
                        }
                    }
                }
        }
        return super.onTouchEvent(event);
    }

    class Word implements Comparable<Word>{
        String text;
        double freq;
        Word(String text, double freq){
            this.text = text;
            this.freq = freq;
        }

        @Override
        public int compareTo(Word o){
            if (this.freq > o.freq) return -1;
            if (this.freq < o.freq) return 1;
            return 0;
        }
    }
}