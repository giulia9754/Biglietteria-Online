package tets;

import com.sun.net.httpserver.*;
import java.io.*;
import java.sql.*;
import java.util.*;
import java.net.InetSocketAddress;

public class Main {

	public static void main(String[] args) throws IOException{
		HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
		
		//gestione richiesta GET pagina HOME
		server.createContext("/", new HomePageHandler());
		
		//getsione della richiesta per la vendita dei biglietti
		server.createContext("/venditaBiglietto", new VenditaBigliettoHandler());
		
		//gestione per la visualizzazione per il riepilogo dei biglietti
		server.createContext("/riepilogoBiglietti", new RiepilogoBigliettiHandler());
		
		//avvio del server
		server.start();
		System.out.println("Server per la gestione della biglietteria avviato su porta: 8080.");

	}
	
	static class VenditaBigliettoHandler implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			//è l'unico metodo che deve gestire sia get che post
			//get o post?
			if(exchange.getRequestMethod().equalsIgnoreCase("GET")) {
				//creazione del DOM con il form per la vendita del biglietto
				String htmlResponse = "<html>"
									+ "<head>"
									+ "<title>Vendita Biglietti</title>"
									+ "<link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css'>"
									+ "<style>"
									+ " body{"
									+ " background-image: url('https://media.meer.com/attachments/7725bcd94bc0c8c2ce869c13a78ea1fabf73f134/store/fill/1090/613/4685b5227116ad2c93693dbe5655b2d30d7daee84bbab531998b864b7166/Stazione-treno-Luogo-reale-eppure-simile-alla-leggenda.jpg');"
									+ " background-size: cover;"
									+ " background-position: center;"
									+ " height: 100vh;"
									+ " color: white;"
									+ " }"
									+ " .container{"
									+ " padding-top: 50px;"	
									+ " }"	
									+ "</style>"
									+ "</head>"
									+ "<body>"
									+ "<div class='container'>"
									+ "<h1 class='mt-5'>Vendita Biglietto</h1>"
									+ "<form id='venditaForm' class='mt-3' method='post' action='/venditaBiglietto'>"
									+ "<div class='form-group'>"
									+ "<label for='data'>Data:</label>"
									+ "<input type='date' class='form-control' id='data' name='data'>"
									+ "</div>"
									+ "<div class='form-group'>"
									+ "<label for='destinazione'>Destinazione:</label>"
									+ "<input type='tetx' class='form-control' id='destinazione' name='destinazione'>"
									+ "</div>"
									+ "<div class='form-group'>"
									+ "<label for='prezzo'>Prezzo:</label>"
									+ "<input type='text' class='form-control' id='prezzo' name='prezzo'>"
									+ "</div>"
									+ "<button type='submit' class='btn btn-primary'>Vendi Biglietto</button>"
									+ "</form>"
									+ "</div>"
									+ "</body>"
									+ "</html>";
				
				//imposta l'intestazione per la risposta al browswer/client
				exchange.getResponseHeaders().set("Content-Type","text/html");
				//imposta lo status code e la lunghezza
				int contentLenght = htmlResponse.getBytes("UTF-8").length;
				exchange.sendResponseHeaders(200, contentLenght);

				//chiamata della libreria per la getsione dello stream dati
				OutputStream os = exchange.getResponseBody();
				os.write(htmlResponse.getBytes());
				os.close();
			}else if(exchange.getRequestMethod().equalsIgnoreCase("POST")) {
				//recuperiamo i dati inviati dal form
				InputStreamReader isr = new InputStreamReader(exchange.getRequestBody(), "utf-8");
				BufferedReader br = new BufferedReader(isr);
				//stringa di tetso per contenere bufferedReader
				String formData = br.readLine();//data=2024-12-06&destinazione=Milano&prezzo=50
				String[] formDataArray = formData.split("&");//{"data=2024-12-06", "destinazione=Milano", "prezzo=50"}
															//		0              1                        2
				String data = formDataArray[0].split("=")[1];
				String destinazione = formDataArray[1].split("=")[1];
				double prezzo = Double.parseDouble(formDataArray[2].split("=")[1]);
				
				// Connessione e scrittura sul database
				try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/biglietteria", "root", "1234@l8q")) {
				    String query = "INSERT INTO biglietti (data, destinazione, prezzo) VALUES (?, ?, ?)";
				    try (PreparedStatement pstmt = conn.prepareStatement(query)) {
				        pstmt.setString(1, data); // Imposta la data
				        pstmt.setString(2, destinazione); // Imposta la destinazione
				        pstmt.setDouble(3, prezzo); // Imposta il prezzo
				        pstmt.executeUpdate(); // Esegue l'inserimento
				    }
				} catch (SQLException e) {
				    System.err.println("Errore durante l'inserimento nel database:");
				    e.printStackTrace();
				}

				// Dopo l'inserimento del biglietto, riportiamo il client sulla Home
				exchange.getResponseHeaders().set("Location", "/");
				exchange.sendResponseHeaders(302, -1);
			}
		}
	}

	
	static class RiepilogoBigliettiHandler implements HttpHandler {
		@Override
		public void handle(HttpExchange exchange) throws IOException{
			//connessione al database e il recupero dei biglietti
			List<String> biglietti = new ArrayList<>();
			try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/biglietteria", "root", "1234@l8q")){
				String query = "SELECT * FROM biglietti";
				try(PreparedStatement pstmt = conn.prepareStatement(query)){
					ResultSet rs = pstmt.executeQuery();
					while(rs.next()) {
						String data = rs.getString("data");
						String destinazione = rs.getString("destinazione");
						double prezzo = rs.getDouble("prezzo");
						
						//aggiungiamo elementi all'array
						biglietti.add("Data: " + data + " -Destinazione: " + destinazione + " -Prezzo: " + prezzo);
					}
				}
				
				}catch(SQLException e) {
					e.printStackTrace();
				}
				
			//preparazione della risposta HTML
			StringBuilder response = new StringBuilder();
			response.append("<html><body>");
			response.append("<h1>Riepilogo Biglietti Venduti: </h1> ");
			for(String biglietto : biglietti) {
				response.append("<p>").append(biglietto).append("</p>");
			}
			response.append("</body></html>");
			
			//prepariamo  l'intestazione ed il flusso dati per il client
			int contentLenght = response.toString().getBytes("UTF-8").length;
			exchange.sendResponseHeaders(200, response.length());
			OutputStream os = exchange.getResponseBody();
			os.write(response.toString().getBytes());
		}
	}

	static class HomePageHandler implements HttpHandler{
		@Override
		public void handle(HttpExchange exchange) throws IOException {
			String htmlResponse = "<html>"
					+ "<head>"
					+ "<title>Biglietteria della Stazione</title>"
					+ "<link rel='stylesheet' href='https://maxcdn.bootstrapcdn.com/bootstrap/4.5.2/css/bootstrap.min.css'>"
					+ "<style>"
					+ " body{"
					+ " background-image: url('https://media.meer.com/attachments/7725bcd94bc0c8c2ce869c13a78ea1fabf73f134/store/fill/1090/613/4685b5227116ad2c93693dbe5655b2d30d7daee84bbab531998b864b7166/Stazione-treno-Luogo-reale-eppure-simile-alla-leggenda.jpg');"
					+ " background-size: cover;"
					+ " background-position: center;"
					+ " height: 100vh;"
					+ " color: white;"
					+ " }"
					+ " .container{"
					+ " padding-top: 50px;"	
					+ " }"	
					+ "</style>"
					+ "</head>"
					+ "<body>"
					+ "<h1>Benvenuto Nella Biglietteria Della Stazione</h1>"
					+ "<ul>"
					+ "<li><a href='/venditaBiglietto'>Prenota il tuo biglietto</a></li>"
					+ "<li><a href='/riepilogoBiglietti'>Visualizza i biglietti venduti</a></li>"
					+ "</ul>"
					+ "</body>"
					+ "</html>";
			
			//Imposta la risposta al client
			exchange.getResponseHeaders().set("Content-Type", "text/html");
			//imposta status code
			int contentLength = htmlResponse.getBytes("UTF-8").length;
			exchange.sendResponseHeaders(200, contentLength);
			//scrivi il flusso dati
			OutputStream os = exchange.getResponseBody();
			os.write(htmlResponse.getBytes());
			os.close();
		}
	}
}