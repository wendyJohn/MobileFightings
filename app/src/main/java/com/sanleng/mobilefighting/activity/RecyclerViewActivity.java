package com.sanleng.mobilefighting.activity;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.loopj.android.http.RequestParams;
import com.sanleng.mobilefighting.R;
import com.sanleng.mobilefighting.adapter.RecyclerViewAdapter;
import com.sanleng.mobilefighting.net.NetCallBack;
import com.sanleng.mobilefighting.net.RequestUtils;
import com.sanleng.mobilefighting.net.URLs;
import com.sanleng.mobilefighting.util.PreferenceUtils;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 视频列表
 *
 * @author Qiaoshi
 */
public class RecyclerViewActivity extends Activity {
    private ListView listView;
    private RecyclerViewAdapter recyclerViewAdapter;
    private ProgressDialog dialog;
    private List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
    private List<Map<String, Object>> allList = new ArrayList<Map<String, Object>>();
    private int pageNo = 1;// 设置pageNo的初始化值为1，即默认获取的是第一页的数据。
    private int allpage;
    private boolean is_divPage;// 是否进行分页操作
    private boolean finish = true;// 是否加载完成;
    private RelativeLayout r_back;// 是否加载完成;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recyclerview);

        listView = (ListView) findViewById(R.id.listview);
        r_back = (RelativeLayout) findViewById(R.id.r_back);
        r_back.setOnClickListener(new MyOnClickListener(0));

        recyclerViewAdapter = new RecyclerViewAdapter();
        dialog = new ProgressDialog(this);
        dialog.setMessage("正在加载中...");
        /**
         * 开始获取数据
         */
        new MyTask().execute(URLs.Article_URL + pageNo + "&pageSize=" + 50 + "&publicitytype=3"
                + "&platformkey=app_firecontrol_owner" + "&username="
                + PreferenceUtils.getString(RecyclerViewActivity.this, "MobileFig_username"));

        listView.setOnScrollListener(new OnScrollListener() {
            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
                /**
                 * 当分页操作is_divPage为true时、滑动停止时、且pageNo<=allpage（ 这里因为服务端有allpage页数据）时，加载更多数据。
                 */
                if (is_divPage && scrollState == OnScrollListener.SCROLL_STATE_IDLE && pageNo <= allpage && finish) {
                    finish = false;
                    new MyTask().execute(URLs.Article_URL + pageNo + "&pageSize=" + 50 + "&publicitytype=3"
                            + "&platformkey=app_firecontrol_owner" + "&username="
                            + PreferenceUtils.getString(RecyclerViewActivity.this, "MobileFig_username"));
                } else if (pageNo > allpage && finish) {
                    finish = false;
                    // 如果pageNo>allpage则表示，服务端没有更多的数据可供加载了。
                    Toast.makeText(RecyclerViewActivity.this, "加载完了！", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                is_divPage = (firstVisibleItem + visibleItemCount == totalItemCount);
            }
        });

        /**
         * 每个item的点击事件
         */
        listView.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String record_url = allList.get(position).get("record_url").toString();
                String myid = allList.get(position).get("id").toString();
                String name = allList.get(position).get("name").toString();
                String category = allList.get(position).get("category").toString();
                String frequency = allList.get(position).get("frequency").toString();

                loadData(myid, name, category, frequency, record_url);

//				// 带进度条拖放功能
//				Intent intent = new Intent(RecyclerViewActivity.this, VideoSurfaceActivity.class);
//				intent.putExtra("record_url", record_url);
//				intent.putExtra("url", content);
//				intent.putExtra("name", name);
//				intent.putExtra("category", category);
//				intent.putExtra("frequency", frequency);
//				startActivity(intent);

                // 不带进度条拖放功能
//				Intent intent = new Intent(RecyclerViewActivity.this, RercyclerItemctivity.class);
//				intent.putExtra("record_url", record_url);
//				intent.putExtra("url", content);
//				intent.putExtra("name", name);
//				intent.putExtra("category", category);
//				intent.putExtra("frequency", frequency);
//				startActivity(intent);

            }
        });
    }

    /**
     * MyTask继承线程池AsyncTask用来网络数据请求、json解析、数据更新等操作。
     */
    class MyTask extends AsyncTask<String, Void, String> {
        /**
         * 数据请求前显示dialog。
         */
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            dialog.show();
        }

        /**
         * 在doInBackground方法中，做一些诸如网络请求等耗时操作。
         */
        @Override
        protected String doInBackground(String... params) {
            return RequestData();
        }

        /**
         * 在该方法中，主要进行一些数据的处理，更新。
         */
        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            if (result != null) {
                List<Map<String, Object>> list = JSONAnalysis(result);
                // 如果获取的result数据不为空，那么对其进行JSON解析。并显示在手机屏幕上。
                allList.addAll(list);
                recyclerViewAdapter.bindData(RecyclerViewActivity.this, allList);
                if (pageNo == 1) {
                    // 没有数据就提示暂无数据。
                    listView.setEmptyView(findViewById(R.id.nodata));
                    listView.setAdapter(recyclerViewAdapter);
                }
                recyclerViewAdapter.notifyDataSetChanged();
                pageNo++;
                finish = true;
                dialog.dismiss();
            } else if (result == null) {
                Toast.makeText(RecyclerViewActivity.this, "请求数据失败...", Toast.LENGTH_LONG).show();
            }
            dialog.dismiss();
        }
    }

    /**
     * 网络数据请求
     *
     * @return
     */
    public String RequestData() {
        HttpGet get = new HttpGet(URLs.Article_URL + pageNo + "&pageSize=" + 50 + "&publicitytype=3"
                + "&platformkey=app_firecontrol_owner" + "&username="
                + PreferenceUtils.getString(RecyclerViewActivity.this, "MobileFig_username"));
        HttpClient client = new DefaultHttpClient();
        StringBuilder builder = null;
        try {
            HttpResponse response = client.execute(get);
            if (response.getStatusLine().getStatusCode() == 200) {
                InputStream inputStream = response.getEntity().getContent();
                BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                builder = new StringBuilder();
                String s = null;
                for (s = reader.readLine(); s != null; s = reader.readLine()) {
                    builder.append(s);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return builder.toString();

    }

    /**
     * JSON解析
     *
     * @param result
     * @return
     */
    public List<Map<String, Object>> JSONAnalysis(String result) {
        System.out.println("数据请求成功" + result);
        try {
            int length = 50;
            int SIZE = 0;
            JSONObject jsonObject = new JSONObject(result);
            String msg = jsonObject.getString("msg");
            if (msg.equals("获取成功")) {
                String data = jsonObject.getString("data");
                JSONObject objects = new JSONObject(data);
                String listsize = objects.getString("total");
                SIZE = Integer.parseInt(listsize);
                String mylist = objects.getString("list");
                JSONArray array = new JSONArray(mylist);
                JSONObject object;
                for (int i = 0; i < array.length(); i++) {
                    object = (JSONObject) array.get(i);
                    String id = object.optString("ids");
                    String name = object.optString("name");
                    String category = object.optString("subject_type");
                    String frequency = object.optString("browse_times");
                    String cover_img = object.optString("cover_img");
                    String record_url = object.optString("record_url");

                    Map<String, Object> map = new HashMap<String, Object>();
                    map.put("id", id);
                    map.put("name", name);
                    map.put("category", "专题类别：" + category);
                    map.put("frequency", frequency + "次");
                    map.put("picname_hospital_s", "http://10.101.80.113:8080" + cover_img);
                    map.put("record_url", "http://10.101.80.113:8080" + record_url);
                    list.add(map);
                }

                if (SIZE % length == 0) {
                    allpage = SIZE / length;
                } else {
                    allpage = SIZE / length + 1;
                }

            } else {

            }

        } catch (JSONException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return list;
    }

    private class MyOnClickListener implements OnClickListener {
        private int index = 0;

        public MyOnClickListener(int i) {
            index = i;
        }

        public void onClick(View v) {
            switch (index) {
                case 0:
                    finish();
                    break;
            }
        }
    }

    // 获取视频详情数据（HTML）
    private void loadData(String id, final String name, final String category, final String frequency,
                          final String record_url) {
        RequestParams params = new RequestParams();
        params.put("ids", id);
        params.put("username", PreferenceUtils.getString(RecyclerViewActivity.this, "MobileFig_username"));
        params.put("platformkey", "app_firecontrol_owner");

        RequestUtils.ClientPost(URLs.ArticleLtem_URL, params, new NetCallBack() {
            @Override
            public void onStart() {
                super.onStart();
            }

            @Override
            public void onMySuccess(String result) {
                if (result == null || result.length() == 0) {
                    return;
                }
                System.out.println("数据请求成功" + result);

                try {
                    JSONObject jsonObject = new JSONObject(result);
                    String msg = jsonObject.getString("msg");
                    if (msg.equals("获取成功")) {
                        String data = jsonObject.getString("data");
                        JSONObject objects = new JSONObject(data);
                        String content = objects.getString("content");
                        // 带进度条拖放功能
                        Intent intent = new Intent(RecyclerViewActivity.this, VideoSurfaceActivity.class);
                        intent.putExtra("record_url", record_url);
                        intent.putExtra("url", "http://10.101.80.113:8080/" + content);
                        intent.putExtra("name", name);
                        intent.putExtra("category", category);
                        intent.putExtra("frequency", frequency);
                        startActivity(intent);

                    }
                } catch (JSONException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }

            @Override
            public void onMyFailure(Throwable arg0) {
            }
        });
    }

}
