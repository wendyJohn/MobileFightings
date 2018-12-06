package com.sanleng.mobilefighting.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.sanleng.mobilefighting.R;
import com.sanleng.mobilefighting.bean.ArchitectureBean;

import java.util.List;

/**
 * 设备数据适配器
 *
 * @author QiaoShi
 *
 */
@SuppressLint("ResourceAsColor")
public class DeviceItemAdapter extends BaseAdapter {

	private List<ArchitectureBean> mList;
	private Context mContext;

	/**
	 * bindData用来传递数据给适配器。
	 *
	 * @list
	 */
	public void bindData(Context mContext, List<ArchitectureBean> mList) {
		this.mList = mList;
		this.mContext = mContext;
	}

	@Override
	public int getCount() {
		return mList.size();

	}

	@Override
	public Object getItem(int position) {
		return position;
	}

	@Override
	public long getItemId(int position) {
		return position;
	}

	@SuppressWarnings("deprecation")
	@Override
	public View getView(final int position, View convertView, ViewGroup parent) {
		Holder holder;

		if (convertView == null) {
			convertView = LayoutInflater.from(mContext).inflate(R.layout.device_items, null);
			holder = new Holder();
			holder.name = (TextView) convertView.findViewById(R.id.name);
			convertView.setTag(holder);
		} else {
			holder = (Holder) convertView.getTag();
		}

		holder.name.setText(mList.get(position).getName());
		return convertView;
	}

	class Holder {
		TextView name;

	}
}
