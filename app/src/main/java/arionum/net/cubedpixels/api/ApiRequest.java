package arionum.net.cubedpixels.api;

import org.json.JSONObject;

import arionum.net.cubedpixels.views.HomeView;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

public class ApiRequest {

	public static void requestFeedback(final RequestFeedback feedback, final String option, final Argument... data) {
		//CREATE ASYNC THREAD WITH JSON RESPONSE
		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					String afterwards = "";
					for (Argument a : data) {
						afterwards += "&" + a.getName() + "=" + a.getData();
					}
					URL url = new URL(HomeView.getCurrentPeer() + "/api.php?q=" + option + afterwards);
					String response = "";
					//DOWNLOAD URL BYTES
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					BufferedInputStream in = new BufferedInputStream(url.openStream());
					byte data[] = new byte[1024];
					int count;
					while ((count = in.read(data, 0, 1024)) != -1) {
						baos.write(data, 0, count);
						try {
							//TRY TO PREFETCH
							String s = new String(baos.toByteArray());
							feedback.onPreFetch(new JSONObject(s.substring(0, s.lastIndexOf("}") + 1) + "]}"));
						} catch (Exception e) {
							e.printStackTrace();
						}
					}
					//FULL RESPONSE
					response = new String(baos.toByteArray());
					//PARSE JSON
					JSONObject obj = new JSONObject(response.toString());
					feedback.onFeedback(obj);
				} catch (Exception e) {
					e.printStackTrace();
					feedback.onFeedback(null);
				}
			}
		}).start();
	}

	public static abstract class RequestFeedback {
		public abstract void onFeedback(JSONObject object);

		public void onPreFetch(JSONObject object) {
		};
	}

	public static class Argument {
		private String name;
		private String data;

		public Argument(String name, Object data) {
			this.name = name;
			this.data = data.toString();
		}

		public String getName() {
			return name;
		}

		public String getData() {
			return data;
		}
	}
}
