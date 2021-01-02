package me.simplq;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ListAdapter;
import android.widget.TextView;

import java.util.List;

import me.simplq.pojo.Queue;

public class QueueListAdapter extends BaseAdapter implements ListAdapter {
    private final List<Queue> list;
    private final Context context;

    public QueueListAdapter(List<Queue> list, Context context) {
        this.list = list;
        this.context = context;
    }

    @Override
    public int getCount() {
        return list.size();
    }

    @Override
    public Object getItem(int pos) {
        return list.get(pos);
    }

    @Override
    public long getItemId(int pos) {
        return 0;
    }

    @Override
    public View getView(final int position, View convertView, ViewGroup parent) {
        View view = convertView;
        if (view == null) {
            LayoutInflater inflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            view = inflater.inflate(R.layout.queue_item, null);
        }

        //Handle TextView and display string from your list
        TextView tvQueueName = (TextView) view.findViewById(R.id.tvQueueName);
        tvQueueName.setText(list.get(position).getName());

        //Handle buttons and add onClickListeners
        Button button = (Button) view.findViewById(R.id.btn);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent browserIntent = new Intent(Intent.ACTION_VIEW,
                        Uri.parse("https://simplq.me/queue/" + list.get(position).getId()));
                context.startActivity(browserIntent);
            }
        });

        return view;
    }
}