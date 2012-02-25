package net.diva.browser.extra.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import android.app.AlertDialog;
import android.app.ListActivity;
import android.app.ProgressDialog;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.database.Cursor;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import au.com.bytecode.opencsv.CSVReader;

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

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {
		final File file = (File) l.getAdapter().getItem(position);
		TextView text = (TextView) v.findViewById(android.R.id.text1);

		AlertDialog.Builder b = new AlertDialog.Builder(this);
		b.setMessage(getString(R.string.confirm_import, text.getText()));
		b.setNegativeButton(android.R.string.cancel, null);
		b.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				new Importer(BackupListActivity.this).execute(file);
			}
		});
		b.show();
	}

	private static class Importer extends AsyncTask<File, Void, CharSequence> {
		private static final Uri URI_HISTORY = Uri.parse("content://net.diva.browser.history/plays");
		private static final String[] COLUMNS = new String[] {
			"music_title",
			"rank",
			"play_date",
			"play_place",
			"clear_status",
			"achievement",
			"score",
			"cool", null,
			"fine", null,
			"safe", null,
			"sad", null,
			"worst", null,
			"combo",
			"challange_time",
			"hold",
			"trial",
			"trial_result",
			"module1",
			"module2",
			"button_se",
			"skin",
			"lock",
		};

		private Context m_context;
		private ProgressDialog m_progress;

		private Map<String, String> m_musics;
		private Map<String, String> m_modules;
		private Map<String, String> m_buttons;
		private Map<String, String> m_skins;

		private Importer(Context context) {
			m_context = context;
			m_progress = new ProgressDialog(context);
		}

		private Map<String, String> loadMap(Uri uri, String...colomns) {
			Map<String, String> map = new HashMap<String, String>();
			Cursor c = m_context.getContentResolver().query(uri, colomns, null, null, null);
			try {
				while (c.moveToNext())
					map.put(c.getString(0), c.getString(1));
			}
			finally {
				c.close();
			}
			return map;
		}

		private void loadMaps() {
			Uri.Builder b = Uri.parse("content://net.diva.browser.store/").buildUpon();
			m_musics = loadMap(b.path("musics").build(), "id", "title");
			m_modules = loadMap(b.path("modules").build(), "id", "name");
			m_buttons = loadMap(b.path("button_ses").build(), "id", "name");
			m_skins = loadMap(b.path("skins").build(), "id", "name");
		}

		private boolean importHistory(String[] data) {
			data[0] = m_musics.get(data[0]);
			data[2] = data[2] + "000";
			data[22] = m_modules.get(data[22]);
			data[23] = m_modules.get(data[23]);
			data[24] = m_buttons.get(data[24]);
			data[25] = m_skins.get(data[25]);
			ContentValues values = new ContentValues();
			for (int i = 0; i < data.length; ++i) {
				if (COLUMNS[i] != null)
					values.put(COLUMNS[i], data[i]);
			}
			return m_context.getContentResolver().insert(URI_HISTORY, values) != null;
		}

		@Override
		protected void onPreExecute() {
			m_progress.setIndeterminate(true);
			m_progress.setMessage(m_context.getText(R.string.importing));
			m_progress.show();
		}

		@Override
		protected CharSequence doInBackground(File... args) {
			CSVReader reader = null;
			try {
				reader = new CSVReader(new InputStreamReader(new FileInputStream(args[0]), "UTF-8"));
				loadMaps();

				int imported = 0;
				for (String[] values; (values = reader.readNext()) != null;) {
					if (importHistory(values))
						++imported;
				}
				if (imported > 0)
					return m_context.getString(R.string.history_imported, imported);
				else
					return m_context.getText(R.string.no_history_imported);
			}
			catch (Exception e) {
				e.printStackTrace();
				return e.getMessage();
			}
			finally {
				if (reader != null)
					try { reader.close(); } catch (IOException e) {}
			}
		}

		@Override
		protected void onPostExecute(CharSequence message) {
			m_progress.dismiss();
			Toast.makeText(m_context, message, Toast.LENGTH_LONG).show();
		}
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
