package net.diva.browser.extra.importer;

import java.io.File;
import java.io.FilenameFilter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;

import android.app.ListActivity;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

public class BackupListActivity extends ListActivity {
	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		final File dir = new File(Environment.getExternalStorageDirectory().getPath() + "/net.diva.browser/");
		final File[] files = dir.listFiles(
				new FilenameFilter() {
					public boolean accept(File dir, String name) {
						return name.matches("DdNB_history_\\d{10}_exported\\.csv$");
					}
				});

		Arrays.sort(files, new Comparator<File>() {
			@Override
			public int compare(File lhs, File rhs) {
				return rhs.compareTo(lhs);
			}
		});
		setListAdapter(new MyAdapter(this, files));
	}

	private static class MyAdapter extends ArrayAdapter<File> {
		private SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy年MM月dd日 HH:mm:ss");

		public MyAdapter(Context context, File[] files) {
			super(context, android.R.layout.simple_list_item_1, android.R.id.text1, files);
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {
			final View view = super.getView(position, convertView, parent);
			TextView text = (TextView) view.findViewById(android.R.id.text1);
			text.setText(DATE_FORMAT.format(new Date(1000L * Integer.valueOf(getItem(position).getName().substring(13, 23)))));
			return view;
		}
	}
}
