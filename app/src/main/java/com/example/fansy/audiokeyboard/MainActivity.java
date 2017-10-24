package com.example.fansy.audiokeyboard;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {
    ImageView keyboard;
    TextView text;
    String currentWord = "";
    ArrayList<Word> dict = new ArrayList();
    ArrayList<Character> seq = new ArrayList<Character>();
    String keys1 = "qwertyuiop";
    String keys2 = "asdfghjkl";
    String keys3 = "zxcvbnm";
    int key_left[] = new int[26];
    int key_right[] = new int[26];
    int key_top[] = new int[26];
    int key_bottom[] = new int[26];
    public void initKeyPosition(){
        key_left['q' - 'a'] = 15;
        key_right['q' - 'a'] = 137;
        key_bottom['q' - 'a'] = 167;
        key_top['q' - 'a'] = 0;
        for (int i = 1; i < keys1.length(); ++i){
            key_left[keys1.charAt(i) - 'a'] = key_left[keys1.charAt(i - 1) - 'a'] + 142;
            key_right[keys1.charAt(i) - 'a'] = key_right[keys1.charAt(i - 1) - 'a'] + 142;
            key_top[keys1.charAt(i) - 'a'] = key_top[keys1.charAt(i - 1) - 'a'];
            key_bottom[keys1.charAt(i) - 'a'] = key_bottom[keys1.charAt(i - 1) - 'a'];
        }

        key_left['a' - 'a'] = 87;
        key_right['a' - 'a'] = 209;
        key_bottom['a' - 'a'] = 354;
        key_top['a' - 'a'] = 187;
        for (int i = 1; i < keys2.length(); ++i){
            key_left[keys2.charAt(i) - 'a'] = key_left[keys2.charAt(i - 1) - 'a'] + 142;
            key_right[keys2.charAt(i) - 'a'] = key_right[keys2.charAt(i - 1) - 'a'] + 142;
            key_top[keys2.charAt(i) - 'a'] = key_top[keys2.charAt(i - 1) - 'a'];
            key_bottom[keys2.charAt(i) - 'a'] = key_bottom[keys2.charAt(i - 1) - 'a'];
        }

        key_left['z' - 'a'] = 230;
        key_right['z' - 'a'] = 352;
        key_bottom['z' - 'a'] = 541;
        key_top['z' - 'a'] = 374;
        for (int i = 1; i < keys3.length(); ++i){
            key_left[keys3.charAt(i) - 'a'] = key_left[keys3.charAt(i - 1) - 'a'] + 142;
            key_right[keys3.charAt(i) - 'a'] = key_right[keys3.charAt(i - 1) - 'a'] + 142;
            key_top[keys3.charAt(i) - 'a'] = key_top[keys3.charAt(i - 1) - 'a'];
            key_bottom[keys3.charAt(i) - 'a'] = key_bottom[keys3.charAt(i - 1) - 'a'];
        }
    }

    public void initKeyboard(){
        keyboard.setOnTouchListener(new View.OnTouchListener(){
            @Override
            public boolean onTouch(View v, MotionEvent event){
                if (event.getPointerCount() >= 2)
                    return false;
                int x = (int)event.getX();
                int y = (int)event.getY();
                switch (event.getActionMasked()){
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_POINTER_DOWN:
                        addToSeq(getKeyByPosition(x, y));
                        //action down
                        break;

                    case MotionEvent.ACTION_MOVE:
                        addToSeq(getKeyByPosition(x, y));
                        //action move
                        break;

                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_POINTER_UP:
                        seq.clear();
                        if (getKeyByPosition(x, y) != KEY_NOT_FOUND) {
                            currentWord += getKeyByPosition(x, y);
                            text.setText(currentWord);
                        }
                        break;
                }
                return true;
            }
        });
    }

    final int DICT_SIZE = 10000;
    public void initDict(){
        BufferedReader reader = new BufferedReader(new InputStreamReader(getResources().openRawResource(R.raw.dict)));
        String line;
        try{
            int lineNo = 0;
            while ((line = reader.readLine()) != null){
                lineNo++;
                String[] ss = line.split(" ");
                dict.add(new Word(ss[0], Integer.valueOf(ss[1])));
                if (lineNo == DICT_SIZE)
                    break;
            }
            reader.close();
            Log.i("init", "read dictionary finished " + dict.size());
        } catch (Exception e){
            Log.i("error", "read dictionary failed");
        }
    }

    public void addToSeq(char ch){
        if (ch != KEY_NOT_FOUND){
            if (seq.size() == 0 || seq.get(seq.size() - 1) != ch)
                seq.add(ch);
        }
    }

    final char KEY_NOT_FOUND = 0;
    char getKeyByPosition(int x, int y){
        for (int i = 0; i < 26; ++i)
            if (x >= key_left[i] && x <= key_right[i] && y >= key_top[i] && y <= key_bottom[i])
                return (char)('a' + i);
        return KEY_NOT_FOUND;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main_relative);

        keyboard = (ImageView)findViewById(R.id.keyboard);
        text = (TextView)findViewById(R.id.text);

        initKeyPosition();
        initKeyboard();
        initDict();
        //left 0 right 1440 top 1554 bottom 2320
    }

    class Word{
        String text;
        int freq;
        Word(String text, int freq){
            this.text = text;
            this.freq = freq;
        }
    }
}
