package xcomp.ytemoi.smartwatch_ytemoi

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.util.Log
import android.view.View
import com.mizuvoip.jvoip.SipStack
import xcomp.ytemoi.smartwatch_ytemoi.databinding.ActivityMainBinding
import java.text.SimpleDateFormat
import java.util.*

class CallYteMoi : Activity(){



    private lateinit var ctx:Context


    var notifThread: GetNotificationsThread? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ctx = this
        instance = this
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.bellCall.setOnClickListener {
            binding.layoutCall.visibility = View.VISIBLE
            binding.layOutChuong.visibility = View.GONE
        }
        binding.startCall.setOnClickListener {
            sipClient!!.Accept(1)
        }
        InnitSipClient()
    }

    private fun InnitSipClient() {
        val params = "serveraddress=171.244.133.171\r\nusername=1000\r\npassword=1000ytm\r\nloglevel=5";
        if(sipClient == null){
            //innit sip
            sipClient = SipStack()
            sipClient!!.Init(ctx)
            sipClient!!.SetParameters(params)

            //start my event listener thread

            //start my event listener thread
            notifThread = GetNotificationsThread()
            notifThread!!.start()

            //start the SIP engine

            //start the SIP engine
            sipClient!!.Start()
            //mysipclient.Register();
        }
    }
    class GetNotificationsThread : Thread() {
        var sipnotifications: String? = ""
        override fun run() {
            try {
                try {
                    currentThread().priority = 4
                } catch (e: Throwable) {
                } //we are lowering this thread priority a bit to give more chance for our main GUI thread
                while (!terminateNotifThread) {
                    try {
                        sipnotifications = ""
                        if (sipClient != null) {
                            //get notifications from the SIP stack
                            sipnotifications = sipClient!!.GetNotificationsSync()
                            if (sipnotifications != null && sipnotifications!!.length > 0) {
                                // send notifications to Main thread using a Handler
                                val messageToMainThread = Message()
                                val messageData = Bundle()
                                messageToMainThread.what = 0
                                messageData.putString("notifmessages", sipnotifications)
                                messageToMainThread.data = messageData
                                NotifThreadHandler?.sendMessage(
                                    messageToMainThread
                                )
                            }
                        }
                        if ((sipnotifications == null || sipnotifications!!.length < 1) && !terminateNotifThread) {
                            //some error occured. sleep a bit just to be sure to avoid busy loop
                            sleep(1)
                        }
                        continue
                    } catch (e: Throwable) {
                        Log.e(
                            LOGTAG,
                            "ERROR, WorkerThread on run()intern",
                            e
                        )
                    }
                    if (!terminateNotifThread) {
                        sleep(10)
                    }
                }
            } catch (e: Throwable) {
                Log.e(LOGTAG, "WorkerThread on run()")
            }
        }
    }

    //process notificatins phrase 1: split by line (we can receive multiple notifications separated by \r\n)
    var notarray: Array<String?>? = null
    fun ReceiveNotifications(notifs: String?) {
        if (notifs == null || notifs.length < 1) return
        notarray = notifs.split("\r\n").toTypedArray()
        if (notarray == null || notarray!!.size < 1) return
        for (i in notarray!!.indices) {
            if (notarray!![i] != null && notarray!![i]!!.length > 0) {
                if (notarray!![i]!!.indexOf("WPNOTIFICATION,") == 0) notarray!![i] = notarray!![i]!!
                    .substring(15) //remove the WPNOTIFICATION, prefix
                ProcessNotifications(notarray!![i])
            }
        }
    }

    //process notificatins phrase 2: processing notification strings
    fun ProcessNotifications(notification: String?) {
        DisplayStatus(notification) //we just display them in this simple test application
        //see the Notifications section in the documentation about the possible messages (parse the notification string and process them after your needs)

        /*
        some example code for notification parsing:

        if (notification.indexOf("WPNOTIFICATION,") == 0)  //remove WPNOTIFICATION prefix
        {
            notification = notification.substring(("WPNOTIFICATION,").length());
        }

        String[] params = notification.split(",");
        if(params.length < 2) return;
        notification = notification.substring(notification.indexOf(','));  //keep only the rest in the notification variable
        params = IncreaseArray(params,20);  //make sure that we have at least 20 parameters and none of them is null to avoid length and null checks below

        if(params[0].equals("STATUS"))
        {
            int line = StringToInt(params[1],0);
            if(line != -1) return;  //we handle only the global state. See the "Multiple lines" FAQ point in the documentation if you wish to handle individual lines explicitely
            int endpointtype = StringToInt(params[5],0);
            if(endpointtype == 2) //incoming call
            {
                DisplayStatus("Incoming call from "+params[3]+" "+params[6]);
                mysipclient.Accept(-1);  //auto accept incoming call. you might disaplay ACCEPT / REJECT buttons instead
            }
        }
        else if(params[0].equals("POPUP"))
        {
            Toast.makeText(this, notification, Toast.LENGTH_LONG).show();
        }
        //else parse other parameters as needed. See the Notifications section in the documentation for the details.
        */
    }

    fun DisplayStatus(stat: String?) {
        if (stat == null) return
        DisplayLogs("Status: $stat")
        if("Ringing" in stat)
        {
            binding.layoutCall.visibility = View.VISIBLE
            binding.layOutChuong.visibility = View.GONE


        }
        else if("Call" in stat){
            binding.layOutChuong.visibility = View.VISIBLE
            binding.layoutCall.visibility = View.GONE
        }
    }
    fun DisplayLogs(logmsg: String?) {
        var logmsg = logmsg
        if (logmsg == null || logmsg.length < 1) return
        if (logmsg.length > 2500) logmsg = logmsg.substring(0, 300) + "..."
        logmsg = """
             [${
            SimpleDateFormat("HH:mm:ss:SSS")
                .format(Calendar.getInstance(TimeZone.getTimeZone("GMT")).time)
        }] $logmsg
             
             """.trimIndent()
        Log.v(LOGTAG, logmsg)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroy() {
        super.onDestroy()
        DisplayLogs("ondestroy")
        terminateNotifThread = true
        if (sipClient != null) {
            DisplayLogs("Stop SipStack")
            sipClient!!.Stop(true)
        }
        sipClient?.Exit()
        sipClient = null
        notifThread = null
    }

    companion object{
        private lateinit var binding: ActivityMainBinding
        public lateinit var instance:CallYteMoi
        var terminateNotifThread = false
        private var sipClient: SipStack? = null
        private val LOGTAG = "AJVoIP"
        public var NotifThreadHandler: Handler? = object : Handler() {
            override fun handleMessage(msg: Message) {
                try {
                    if (msg == null || msg.data == null) return
                    val resBundle = msg.data
                    val receivedNotif = msg.data.getString("notifmessages")
                    if (receivedNotif != null && receivedNotif.length > 0) instance.ReceiveNotifications(
                        receivedNotif
                    )
                } catch (e: Throwable) {
                    Log.e(
                        LOGTAG,
                        "NotifThreadHandler handle Message"
                    )
                }
            }
        }
    }
}