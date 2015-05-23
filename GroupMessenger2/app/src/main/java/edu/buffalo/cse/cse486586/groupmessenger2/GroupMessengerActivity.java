package edu.buffalo.cse.cse486586.groupmessenger2;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.lang.reflect.Array;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Timer;
import java.util.TimerTask;

/**
 * GroupMessengerActivity is the main Activity for the assignment.
 * 
 * @author stevko
 *
 */
public class GroupMessengerActivity extends Activity {
    static final String TAG = GroupMessengerActivity.class.getSimpleName();
    static final String REMOTE_PORT0 = "11108";
    static final String REMOTE_PORT1 = "11112";
    static final String REMOTE_PORT2 = "11116";
    static final String REMOTE_PORT3 = "11120";
    static final String REMOTE_PORT4 = "11124";
    ArrayList<Long> tstp;
    static final Integer p0=4,p1=1,p2=3,p3=0,p4=2;

    static final int SERVER_PORT = 10000;
    double seq_no=0;

    int counter=0,o_seq=0, myindex;
    public static ArrayList<Integer> dead;
    Comparator<Map.Entry<String, Double>> comparator = new EntryComparator();
    Comparator<Map.Entry<String, ArrayList<Double>>> comparator_s = new EntryComparator_s();

    PriorityQueue<Map.Entry<String, ArrayList<Double>>> sender_queue,queue;
    private Uri providerUri=Uri.parse("content://edu.buffalo.cse.cse486586.groupmessenger2.provider");
    String myPort;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_group_messenger);

        queue = new PriorityQueue<Map.Entry<String, ArrayList<Double>>>(1,comparator_s);
        sender_queue = new PriorityQueue<Map.Entry<String, ArrayList<Double>>>(1,comparator_s);
        dead=new ArrayList<Integer>();
        tstp=new ArrayList<Long>();
        tstp.add(System.currentTimeMillis());
        tstp.add(System.currentTimeMillis());
        tstp.add(System.currentTimeMillis());
        tstp.add(System.currentTimeMillis());
        tstp.add(System.currentTimeMillis());


        TelephonyManager tel = (TelephonyManager) this.getSystemService(Context.TELEPHONY_SERVICE);
        String portStr = tel.getLine1Number().substring(tel.getLine1Number().length() - 4);
        myPort = String.valueOf((Integer.parseInt(portStr) * 2));
        myindex=(Integer.parseInt(myPort)%10)/2;
        counter=100 * (myindex+1);
        try {
            /*
             * Create a server socket as well as a thread (AsyncTask) that listens on the server
             * port.
             *
             * AsyncTask is a simplified thread construct that Android provides. Please make sure
             * you know how it works by reading
             * http://developer.android.com/reference/android/os/AsyncTask.html
             */
            ServerSocket serverSocket = new ServerSocket(SERVER_PORT);

            new ServerTask().executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, serverSocket);
        } catch (IOException e) {
            /*
             * Log is a good way to debug your code. LogCat prints out all the messages that
             * Log class writes.
             *
             * Please read http://developer.android.com/tools/debugging/debugging-projects.html
             * and http://developer.android.com/tools/debugging/debugging-log.html
             * for more information on debugging.
             */
            Log.e(TAG, "Can't create a ServerSocket");
            return;
        }

        /*
         * TODO: Use the TextView to display your messages. Though there is no grading component
         * on how you display the messages, if you implement it, it'll make your debugging easier.
         */
        TextView tv = (TextView) findViewById(R.id.textView1);
        tv.setMovementMethod(new ScrollingMovementMethod());
        
        /*
         * Registers OnPTestClickListener for "button1" in the layout, which is the "PTest" button.
         * OnPTestClickListener demonstrates how to access a ContentProvider.
         */
        findViewById(R.id.button1).setOnClickListener(
                new OnPTestClickListener(tv, getContentResolver()));
        
        /*
         * TODO: You need to register and implement an OnClickListener for the "Send" button.
         * In your implementation you need to get the message from the input box (EditText)
         * and send it to other AVDs.
         */

        final EditText et=(EditText)findViewById(R.id.editText1);

        Button send =(Button)findViewById(R.id.button4);
        send.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                counter++;
                String msg=et.getText().toString();
                if(!dead.contains(p0))
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT0+"_"+counter);
                if(!dead.contains(p1))
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT1+"_"+counter);
                if(!dead.contains(p2))
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT2+"_"+counter);
                if(!dead.contains(p3))
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT3+"_"+counter);
                if(!dead.contains(p4))
                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg, REMOTE_PORT4+"_"+counter);

                TextView localTextView = (TextView) findViewById(R.id.textView1);
                localTextView.append("\t" + msg); // This is one way to display a string.
                TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                remoteTextView.append("\n");
                et.setText("");
                Log.d("deadcnt",""+dead.size());
                String err="";
                for(int i=0;i<dead.size();i++)
                    err+=dead.get(0)+",";
                Log.d("dead",err);
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.activity_group_messenger, menu);
        return true;
    }

    /***
     * ServerTask is an AsyncTask that should handle incoming messages. It is created by
     * ServerTask.executeOnExecutor() call in SimpleMessengerActivity.
     *
     * Please make sure you understand how AsyncTask works by reading
     * http://developer.android.com/reference/android/os/AsyncTask.html
     *
     * @author stevko
     *
     */
    private class ServerTask extends AsyncTask<ServerSocket, String, Void> {

        @Override
        protected Void doInBackground(ServerSocket... sockets) {

            /*
             * TODO: Fill in your server code that receives messages and passes them
             * to onProgressUpdate().
             * Accept connection from client.
             * As long as data is received, send it to onProgressUpdate
             */
            ServerSocket serverSocket = sockets[0];
            Socket clientSocket = null;
            String text;
            try {


                while(true)
                {
                    clientSocket=serverSocket.accept();
                    String type=null;
                    ObjectInputStream objs= new ObjectInputStream(clientSocket.getInputStream());
                    ArrayList<Object> msgBundle=(ArrayList<Object>) objs.readObject();
                    if(msgBundle!=null){
                        type= (String) msgBundle.get(0);
                    }

                    if(type!=null && !type.contains("test")) {
                        Log.d("type",type);
                        Map.Entry<String, Double> msg= (Map.Entry<String, Double>) msgBundle.get(1);
                        text=msg.getKey();

                        Log.e(TAG, "msg="+text+ ", "+type+","+msg.getValue());

                        if(type.contains("multicast"))
                        {
                                int found=0;
                                if(!type.contains("_")) {
                                    Iterator it = queue.iterator();
                                    while (it.hasNext()) {
                                        Map.Entry<String, ArrayList<Double>> ele = (Map.Entry<String, ArrayList<Double>>) it.next();

                                        if (ele.getValue().get(1) == Double.parseDouble(text)) {
                                            queue.remove(ele);
                                            // found
                                            // Second time
                                            found = 1;
                                            ArrayList<Double> temp = ele.getValue();
                                            temp.set(0, Math.max(msg.getValue(), ele.getValue().get(0)));   //set new seq no
                                            temp.set(2, 1.0);                                                //set deliverable
                                            ele.setValue(temp);
                                            queue.add(ele);
                                        }
                                    }
                                }
                            else
                                if(found==0) {
                                    // First time

                                    String sender_port = type.split("_")[1];
                                    Log.d("myport,s_port", myPort + "," + sender_port);

                                    seq_no=seq_no+1;
                                    double t=((double)myindex+1)/10.0f;
                                    t+=seq_no;
                                    ArrayList<Double> det = new ArrayList<Double>();
                                    det.add(t);            //seq no
                                    det.add(msg.getValue());    //msg id
                                    det.add(Double.valueOf(sender_port));                 //undelivered
                                    Log.d("det0",det.get(0)+","+t);
                                    new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, msg.getValue().toString(), sender_port, t + "");
                                    Map.Entry<String, ArrayList<Double>> ele = new AbstractMap.SimpleEntry<String, ArrayList<Double>>(text, det);
                                    queue.add(ele);
                                }
                        }
                        else if(type.contains("reply")) {
                            String sender_port = type.split("_")[1];
                            int senderid=(Integer.parseInt(sender_port)%10)/2;
                            // operations for the sender
                            Iterator it=sender_queue.iterator();
                            while(it.hasNext())
                            {
                                Map.Entry<String, ArrayList<Double>> ele  = (Map.Entry<String, ArrayList<Double>>) it.next();

                                int in = senderid + 1;
                                int found=0;
                                if(ele.getValue().get(0) == Double.parseDouble(text))     // Msg id
                                {

                                    sender_queue.remove(ele);
                                    found=1;
                                    ArrayList<Double> temp=ele.getValue();
                                    Log.d("in,val",in+","+msg.getValue());
                                    temp.set(in, msg.getValue());   //set new seq no
                                    ele.setValue(temp);

                                    sender_queue.add(ele);
                                }

                                    double max=-1;
                                    for(Integer i=1;i<6;i++)
                                    {
                                        if(!dead.contains(i-1)) {
                                            Log.d("ele[]", i + "," + ele.getValue().get(i));
                                            if(found==1 && in==i)
                                                max = Math.max(max, msg.getValue());
                                            else
                                                max = Math.max(max, ele.getValue().get(i));
                                            if (ele.getValue().get(i) == -1) {
                                                for(Integer j=i;j<6;j++) {
                                                    int rp = 11108 + (j - 1) * 4;
                                                    if(ele.getValue().get(j) == -1 && !dead.contains(j))
                                                        new CheckTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "test", rp + "");
                                                }
                                                max = -1;
                                                break;
                                            }
                                        }
                                    }
                                    if(max!=-1)
                                    {
                                        Log.d("countthis","see");
                                        // multicast again
                                        if(!dead.contains(p0))
                                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ele.getValue().get(0)+"", REMOTE_PORT0, max+"",  "multicast2");
                                        if(!dead.contains(p1))
                                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ele.getValue().get(0)+"", REMOTE_PORT1, max+"",  "multicast2");
                                        if(!dead.contains(p2))
                                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ele.getValue().get(0)+"", REMOTE_PORT2, max+"",  "multicast2");
                                        if(!dead.contains(p3))
                                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ele.getValue().get(0)+"", REMOTE_PORT3, max+"",  "multicast2");
                                        if(!dead.contains(p4))
                                            new ClientTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, ele.getValue().get(0)+"", REMOTE_PORT4, max+"",  "multicast2");
                                    }




                            }

                        }

                        publishProgress(text.trim());
                    }
                    clientSocket.close();
                }



            } catch (InterruptedIOException e) {
                Log.d("dead","timeout_read: "+clientSocket.getRemoteSocketAddress() +","+clientSocket.getPort()+","+clientSocket.getLocalSocketAddress());

                e.printStackTrace();
            } catch (IOException e) {

                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }


            return null;
        }

        protected void onProgressUpdate(String...strings) {
            /*
             * The following code displays what is received in doInBackground().
             */


            while(true) {
                Map.Entry<String, ArrayList<Double>> ele = queue.peek();
                if(ele==null)
                    break;
                int rp=(int)ele.getValue().get(2).doubleValue();

                Integer rpind=(rp%10)/2;
                if(rp==1 || dead.contains(rpind))
                {
                    double sno=ele.getValue().get(0);
                    // Deliver the message
                    String strReceived=ele.getKey();
                    ContentValues keyValueToInsert = new ContentValues();

                    Log.d("MSGS",String.valueOf(ele.getValue().get(0))+ ","+o_seq+" - " + strReceived);

                    // inserting <”key-to-insert”, “value-to-insert”>
                    keyValueToInsert.put("key", o_seq+"");
                    keyValueToInsert.put("value", strReceived);

                    o_seq++;


                    Uri newUri = getContentResolver().insert(
                            providerUri,    // assume we already created a Uri object with our provider URI
                            keyValueToInsert
                    );


                    TextView remoteTextView = (TextView) findViewById(R.id.textView1);
                    remoteTextView.append(strReceived + "\t\n");
                    TextView localTextView = (TextView) findViewById(R.id.textView1);
                    localTextView.append("\n");
                    queue.poll();
                }
                else
                {
                    if(!myPort.equals(rp)) {
                        Log.d("rp and dc is ", rp + "," + dead.size());
                        new CheckTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR, "test", rp + "");
                    }
                    break;
                }
            }



            return;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class ClientTask extends AsyncTask<String, Void, Void> {
        int senderid;
        String rp;
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String [] msgid = null;
                String remotePort = msgs[1];
                if(remotePort.contains("_")) {
                    msgid=remotePort.split("_");
                    remotePort = msgid[0];
                }
                rp=remotePort;
                senderid=(Integer.parseInt(remotePort)%10)/2;
                Log.e("my_port, to_port",myPort+","+remotePort);
                Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                        Integer.parseInt(remotePort));

                String msgToSend = msgs[0];
                ArrayList<Object> msgBundle=new ArrayList<Object>();
                Map.Entry<String, Double> msg;
                if(msgs.length==2) {

                    msgBundle.add("multicast_"+myPort);
                    msg = new AbstractMap.SimpleEntry(msgs[0], Double.parseDouble(msgid[1]));
                    ArrayList<Double> msgNo = new ArrayList<Double>();
                    msgNo.add(Double.parseDouble(msgid[1]));
                    msgNo.add(-1.0);
                    msgNo.add(-1.0);
                    msgNo.add(-1.0);
                    msgNo.add(-1.0);
                    msgNo.add(-1.0);
                    int found=0;
                    Iterator it=sender_queue.iterator();
                    while(it.hasNext())
                    {
                        Map.Entry<String, ArrayList<Double>> ele = (Map.Entry<String, ArrayList<Double>>) it.next();
                        if(ele.getValue().get(0)==Double.parseDouble(msgid[1])) {
                            found = 1;
                            break;
                        }
                    }
                    if(found==0)
                        sender_queue.add(new AbstractMap.SimpleEntry(msgs[0], msgNo));
                }
                else if(msgs.length==3){
                    msgBundle.add("reply_"+myPort);
                    msg = new AbstractMap.SimpleEntry(msgs[0], Double.parseDouble(msgs[2]));
                }
                else if(msgs.length==4)
                {
                    msgBundle.add("multicast");
                    msg = new AbstractMap.SimpleEntry(msgs[0], Double.parseDouble(msgs[2]));
                }
                else
                {
                    msg=null;
                    Log.d("WRONG","ERROR");
                }

                msgBundle.add(msg);
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.writeObject(msgBundle);

                out.close();
                socket.close();
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                dead.add(senderid);
                Log.e(TAG, "ClientTask socket IOException - dead-sender: "+rp+","+senderid);
                e.printStackTrace();
            }

            return null;
        }
    }

    /***
     * ClientTask is an AsyncTask that should send a string over the network.
     * It is created by ClientTask.executeOnExecutor() call whenever OnKeyListener.onKey() detects
     * an enter key press event.
     *
     * @author stevko
     *
     */
    private class CheckTask extends AsyncTask<String, Void, Void> {
        int senderid;
        String rp;
        @Override
        protected Void doInBackground(String... msgs) {
            try {
                String [] msgid = null;
                String remotePort = msgs[1];
                if(remotePort.contains("_")) {
                    msgid=remotePort.split("_");
                    remotePort = msgid[0];
                }
                rp=remotePort;
                senderid=(Integer.parseInt(remotePort)%10)/2;
                if(System.currentTimeMillis()-tstp.get(senderid)>500 && dead.size()<1) {
                    tstp.set(senderid,System.currentTimeMillis());

                    Log.e("Check, my_port, to_port", myPort + "," + remotePort);
                    Socket socket = new Socket(InetAddress.getByAddress(new byte[]{10, 0, 2, 2}),
                            Integer.parseInt(remotePort));
                    ArrayList<Object> msgBundle = new ArrayList<Object>();

                    msgBundle.add("test_" + myPort);

                    ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                    out.writeObject(msgBundle);

                    out.close();
                    socket.close();
                }
                else
                    Log.d("Check","Checked now only!");
            } catch (UnknownHostException e) {
                Log.e(TAG, "ClientTask UnknownHostException");
            } catch (IOException e) {
                dead.add(senderid);
                Log.e(TAG, "CheckTask socket IOException - dead-sender: "+rp +","+senderid);
                e.printStackTrace();
            }

            return null;
        }
    }

    public class EntryComparator implements Comparator<Map.Entry<String, Double>>
    {
        @Override
        public int compare(Map.Entry<String, Double> lhs, Map.Entry<String, Double> rhs) {

            if(lhs.getValue()<rhs.getValue())
                return -1;
            if(lhs.getValue()>rhs.getValue())
                return 1;

            return 0;
        }
    }
    public class EntryComparator_s implements Comparator<Map.Entry<String, ArrayList<Double>>>
    {
        @Override
        public int compare(Map.Entry<String, ArrayList<Double>> lhs, Map.Entry<String, ArrayList<Double>> rhs) {

            if(lhs.getValue().get(0)<rhs.getValue().get(0))
                return -1;
            if(lhs.getValue().get(0)>rhs.getValue().get(0))
                return 1;

            return 0;
        }
    }
}
