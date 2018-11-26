
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.websocket.OnClose;
import javax.websocket.OnError;
import javax.websocket.OnMessage;
import javax.websocket.OnOpen;
import javax.websocket.Session;
import javax.websocket.server.ServerEndpoint;

@ServerEndpoint("/id2")

public class CustomEndPoint {

	static Map<String, Set<String>> topicMap = new HashMap<>();
	static Session pubSession = null;
	static Session subSession = null;

	private static Thread rateThread; // rate publisher thread

	static {
		// rate publisher thread, generates a new value for USD rate every 2 seconds.
		rateThread = new Thread() {
			public void run() {

				Connection globalConn = null;
				String statusSQL = "SELECT STATUS,SUBJECT FROM STATUS WHERE ID=1";
				String messageSQL = "SELECT MESSAGE FROM TOPIC_MESSAGE WHERE TOPIC = ?";
				PreparedStatement preparedStmt = null;
				try {
					Class.forName("com.mysql.jdbc.Driver");
					globalConn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cse586_lab2", "root",
							"Pooja@007");
					while (true) {
						preparedStmt = globalConn.prepareStatement(statusSQL);
						ResultSet rs = preparedStmt.executeQuery();
						int status = 0;
						String topic = null;
						while (rs != null && rs.next()) {
							status = rs.getInt("STATUS");
							topic = rs.getString("SUBJECT");
						}
						if (status == 1) {
							addTopic(topic);
							updateStatusTable(globalConn);
							truncateTable(globalConn, "TOPICS");
						} else if (status == 2) {
							preparedStmt = globalConn.prepareStatement(messageSQL);
							if (topic == null)
								topic = "";
							preparedStmt.setString(1, topic);
							rs = preparedStmt.executeQuery();
							String message = null;
							while (rs != null && rs.next()) {
								message = rs.getString("MESSAGE");
							}
							if (message != null) {
							publish(topic, message);
							updateStatusTable(globalConn);
							truncateTable(globalConn, "TOPICS");
							truncateTable(globalConn, "TOPIC_MESSAGE");
							} else {
								System.out.println("No message to publish");
							}
							
						}
						try {

							sleep(2000);

						} catch (InterruptedException e) {

						}
					}

				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (ClassNotFoundException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} finally {
					try {
						if (globalConn != null)
							globalConn.close();
					} catch (SQLException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}

			};

		};

		rateThread.start();

	}

	@OnMessage
	public void onMessage(Session session, String msg) {
		if (msg != null && msg.length() != 0) {
			String[] mAr = msg.split(" ");
			int len = mAr.length;
			if (len >= 2) {
				if (mAr[0].equals("addTopic")) {
					addTopicAndPersist(mAr[1]);
				} else if (mAr[0].equals("publish")) {
					publishAndPersist(mAr);
				} else if (mAr[0].equals("subscribe")) {
					subscribe(mAr[1], mAr[2]);
				} else if (mAr[0].equals("unsubscribe")) {
					unsubscribe(mAr[1], mAr[2]);
				}
			}
		}
	}

	public static void addTopic(String topic) {
		Set<String> topSet = topicMap.get(topic);
		if (topSet == null) {
			topSet = new HashSet<>();
			topicMap.put(topic, topSet);
			if (subSession != null && subSession.isOpen()) {
				try {
					subSession.getBasicRemote().sendText("Topic " + topic);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	public static void addTopicAndPersist(String topic) {
		Set<String> topSet = topicMap.get(topic);
		if (topSet == null) {
			topSet = new HashSet<>();
			topicMap.put(topic, topSet);
			insertTopicIntoDB(topic);
			if (subSession != null && subSession.isOpen()) {
				try {
					subSession.getBasicRemote().sendText("Topic " + topic);
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

		}
	}

	public static void publish(String topic, String message) {
		System.out.println("Publishing message");
		Set<String> subSet = topicMap.get(topic);
		if (!subSet.isEmpty()) {
			for (String string : subSet) {
				String newMessage = "\n" + new Date(System.currentTimeMillis()) + ": " + topic + " update - " + message
						+ " to " + string;
				if (subSession != null && subSession.isOpen()) {
					try {
						System.out.println(newMessage);
						subSession.getBasicRemote().sendText(newMessage);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
	}

	public static void publishAndPersist(String[] mAr) {
		Set<String> subSet = topicMap.get(mAr[1]);
		StringBuilder b = new StringBuilder();
		if (!subSet.isEmpty()) {
			for (int i = 2; i < mAr.length; i++) {
				b.append(mAr[i]);
				b.append(" ");
			}
			insertMessageIntoDB(mAr[1], b.toString());
			for (String string : subSet) {
				String message = "\n" + new Date(System.currentTimeMillis()) + ": " + mAr[1] + " update - "
						+ b.toString() + " to " + string;
				if (subSession != null && subSession.isOpen()) {
					try {
						subSession.getBasicRemote().sendText(message);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
				}
			}
		}
		else {
			for (int i = 2; i < mAr.length; i++) {
				b.append(mAr[i]);
				b.append(" ");
			}
			insertMessageIntoDB(mAr[1], b.toString());
		}
			
	}

	public static void updateStatusTable(Connection conn) {
		String updateSQL = "UPDATE STATUS SET STATUS = ?, SUBJECT = ? WHERE ID=1";
		PreparedStatement preparedStmt = null;
		try {
			preparedStmt = conn.prepareStatement(updateSQL);
			preparedStmt.setInt(1, 0);
			preparedStmt.setString(2, "none");
			System.out.println("Update status- " + preparedStmt.executeUpdate());
			System.out.println("Update status table to value 0");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (preparedStmt != null)
					preparedStmt.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	public static void truncateTable(Connection conn, String tableName) {
		String truncateSQL = "delete from " + tableName + " where ID=1";
		PreparedStatement preparedStmt = null;
		try {
			preparedStmt = conn.prepareStatement(truncateSQL);
			preparedStmt.execute();
			System.out.println("Truncated table for flag=1 " + tableName);
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			if (preparedStmt != null)
				try {
					preparedStmt.close();
				} catch (SQLException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
		}
	}

	public static void subscribe(String subscriber, String topic) {
		Set<String> topSet = topicMap.get(topic);
		if (topSet != null)
			topSet.add(subscriber);
	}

	public static void unsubscribe(String subscriber, String topic) {
		Set<String> topSet = topicMap.get(topic);
		if (topSet != null && topSet.contains(subscriber))
			topSet.remove(subscriber);
	}

	@OnOpen

	public void open(Session session) {
		Map<String, List<String>> params = session.getRequestParameterMap();

		if (params.get("TYPE") != null && (params.get("TYPE").get(0).equals("PUBLISHER")))
			pubSession = session;
		else
			subSession = session;

		System.out.println("New session opened: " + session.getId());

	}

	@OnError

	public void error(Session session, Throwable t) {

		System.err.println("Error on session " + session.getId());

	}

	@OnClose

	public void closedConnection(Session session) {

		System.out.println("session closed: " + session.getId());

	}

	private static void insertTopicIntoDB(String topic) {
		Connection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cse586_lab2", "root", "Pooja@007");
			String insertSQL = "INSERT INTO TOPICS(ID,NAME) VALUES (-1,?)";
			PreparedStatement preparedStmt = conn.prepareStatement(insertSQL);
			preparedStmt.setString(1, topic);
			preparedStmt.execute();

			String updateSQL = "UPDATE STATUS SET STATUS = ?, SUBJECT = ? WHERE ID = 1";
			preparedStmt = conn.prepareStatement(updateSQL);
			preparedStmt.setInt(1, -1);
			preparedStmt.setString(2, topic);
			preparedStmt.executeUpdate();

			System.out.println("Updated tables for new topic found");

		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

	private static void insertMessageIntoDB(String topic, String message) {
		Connection conn = null;
		try {
			Class.forName("com.mysql.jdbc.Driver");
			conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/cse586_lab2", "root", "Pooja@007");

			String updateSQL = "INSERT INTO TOPIC_MESSAGE(ID,MESSAGE,TOPIC) VALUES(-1,?,?)";
			PreparedStatement preparedStmt = conn.prepareStatement(updateSQL);
			preparedStmt.setString(1, message);
			preparedStmt.setString(2, topic);
			preparedStmt.execute();

			updateSQL = "UPDATE STATUS SET STATUS = ?, SUBJECT = ? WHERE ID = 1";
			preparedStmt = conn.prepareStatement(updateSQL);
			preparedStmt.setInt(1, -2);
			preparedStmt.setString(2, topic);
			preparedStmt.executeUpdate();

			System.out.println("Updated tables for new message published");
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ClassNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} finally {
			try {
				if (conn != null)
					conn.close();
			} catch (SQLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

	}

}
