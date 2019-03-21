package fileProcessor;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import bean.Song;
import dataBaseHelper.ConnectionManager;

public class FileProcessor {
	private static Logger logger = LoggerFactory.getLogger(FileProcessor.class);
	private File[] files = null;
	private String directory;

	public FileProcessor(String directory) {
		this.directory = directory;
	}

	public void init() {
		File file = new File(directory);
		files = file.listFiles();
		if (files.length <= 0) {
			logger.info("directory is empty");
		} else {
			logger.info("init success");
		}
	}

	public void process() {
		ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(10, 30, 5, TimeUnit.MINUTES,
				new LinkedBlockingQueue<Runnable>());
		for (int i = 0; i < files.length; i++) {
			threadPoolExecutor.execute(new DealThread(files[i]));
		}
		threadPoolExecutor.shutdown();
	}

	private class DealThread implements Runnable {
		private PreparedStatement pstm;
		private File file;
		private String sing_uid;
		private List<String> songIdsList = new ArrayList<>();
		private List<String> songNameList = new ArrayList<>();
		private Logger dLogger = LoggerFactory.getLogger(DealThread.class);
		public DealThread(File file) {
			// TODO Auto-generated constructor stub
			this.file = file;
		}

		@Override
		public void run() {
			// TODO Auto-generated method stub
			final String SING_UID = "sing_uid:";
			final String SONGIDSLIST = "songIdsList:";
			final String SONGNAMELIST = "songNameList";
			boolean isIds = false;
			boolean isName = false;
			BufferedReader reader = null;
			try {
				reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
				String line;
				while ((line = reader.readLine()) != null) {
					if (line.startsWith(SING_UID)) {
						sing_uid = line.substring(SING_UID.length()).trim();
						continue;
					}
					if (line.startsWith(SONGIDSLIST)) {
						isIds = true;
						continue;
					}
					if (line.startsWith(SONGNAMELIST)) {
						isName = true;
						isIds = false;
						continue;
					}
					if (isIds) {
						songIdsList.add(line.trim());
					}
					if (isName) {
						songNameList.add(line.trim());
					}
				}
				if (songNameList.size() * songIdsList.size() == 0) {
					dLogger.error("error file:" + file.getName());
				} else {
					String sql ="insert into tb_song values(?,?,?,?,?)";
					Iterator<String> songIter = songIdsList.iterator();
					Iterator<String> nameIter = songNameList.iterator();
					while(songIter.hasNext()&&nameIter.hasNext()){
						Song song = new Song();
						song.setSid(Integer.parseInt(sing_uid));
						song.setId(Integer.parseInt(songIter.next()));
						String name=nameIter.next();
						song.setName(name);
						saveSong(sql,song);
						dLogger.info("insert song:{} success",name);
						song.clean();
					}
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} finally {
				try {
					if (reader != null ) {
						reader.close();
					}
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		private void saveSong(String sql,Song song){
			try {
				pstm=ConnectionManager.getConnectionFromThreadLocal().prepareStatement(sql);
				pstm.setObject(1, song.getId());
				pstm.setObject(2, song.getSid());
				pstm.setObject(3, song.getAid());
				pstm.setString(4, song.getName());
				pstm.setString(5, song.getSongLink());
				pstm.execute();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}finally {
				try {
					pstm.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}

	public static void main(String[] args) {
		FileProcessor processor = new FileProcessor("d:/work/Spider/music.baidu.com/");
		processor.init();
		processor.process();
	}
}
