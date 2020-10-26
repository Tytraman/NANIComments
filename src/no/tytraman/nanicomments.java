package no.tytraman;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class nanicomments {

    public static Path root = Paths.get(".");                        // Chemin du dossier actuel
    public static List<String> KEYS = new ArrayList<>();                // Liste des clés API
    public static List<String> CHANNELS = new ArrayList<>();            // Liste des chaînes

    public static void main(String[] args) {
        // Récupération des clés de l'API YouTube
        File keysFile = new File(root + File.separator + "keys");
        try(BufferedReader reader = new BufferedReader(new FileReader(keysFile))) {
            String line = "";
            while((line = reader.readLine()) != null) {
                KEYS.add(line);
            }
        }catch(IOException e) {
            System.out.println("Aucun fichier \"keys\" trouvé... Création d'un fichier vide...");
            try {
                keysFile.createNewFile();
            }catch(IOException ee) {
                ee.printStackTrace();
            }
        }
        if(KEYS.size() == 0) {
            System.out.println("Aucune clé API n'est utilisée, veuillez en ajouter dans le fichier \"keys\"\nArrêt en cours...");
            System.exit(-1);
        }
        // Récupération de la liste des chaînes à parcourir
        File channelsFile = new File(root + File.separator + "channels");
        try(BufferedReader reader = new BufferedReader(new FileReader(channelsFile))) {
            String line = "";
            while((line = reader.readLine()) != null) {
                CHANNELS.add(line);
            }
        }catch(IOException e) {
            System.out.println("Aucun fichier \"channels\" trouvé... Création d'un fichier vide...");
            try {
                channelsFile.createNewFile();
            }catch(IOException ee) {
                ee.printStackTrace();
            }
        }
        if(CHANNELS.size() == 0) {
            System.out.println("Aucune chaîne n'est parcourue, veuillez en ajouter dans le fichier \"channels\"\nArrêt en cours...");
            System.exit(-2);
        }
        // Création des dossiers nécessaires
        File commentsPath = new File(root + File.separator + "comments");
        commentsPath.mkdirs();

        System.out.println("Veuillez patienter...");
        int index = 0;
        for(String channelID : CHANNELS) {
            System.out.println("[" + (index + 1) + "/" + CHANNELS.size() + "] [" + (index * 100 / CHANNELS.size()) + "%] Récupération des commentaires de " + channelID);
            try(BufferedWriter writer = new BufferedWriter(new FileWriter(root + File.separator + "comments" + File.separator + channelID + ".json"))) {
                writer.write("[\n");
                String nextPageToken = null;
                do {
                    nextPageToken = getComments(channelID, nextPageToken, writer);
                }while(nextPageToken != null);
                getComments(channelID, null, writer);
                writer.write("]\n");
            } catch (IOException e) {
                e.printStackTrace();
            }
            index++;
        }
        System.out.println("Terminé");
    }

    private static String getComments(String channelID, String nextPageToken, BufferedWriter writer) {
        try {
            int currentKey = 0;
            URL url0 = new URL(
                    "https://www.googleapis.com/youtube/v3/commentThreads?" +
                       "key=" + KEYS.get(0) +
                       "&part=replies,snippet" +
                       "&allThreadsRelatedToChannelId=" + channelID +
                       "&maxResults=100" +
                       (nextPageToken != null ? "&pageToken=" + nextPageToken : "")
            );
            HttpURLConnection connection = (HttpURLConnection) url0.openConnection();
            int code = connection.getResponseCode();
            while(code == 400 ||code == 403) {
                connection.disconnect();
                currentKey++;
                if(currentKey < KEYS.size()) {
                    url0 = new URL(
                            "https://www.googleapis.com/youtube/v3/commentThreads?" +
                                    "key=" + KEYS.get(currentKey) +
                                    "&part=replies,snippet" +
                                    "&allThreadsRelatedToChannelId=" + channelID +
                                    "&maxResults=100" +
                                    (nextPageToken != null ? "&pageToken=" + nextPageToken : "")
                    );
                    connection = (HttpURLConnection) url0.openConnection();
                    code = connection.getResponseCode();
                }else {
                    System.out.println("Plus de clé API valide...\nArrêt en cours...");
                    System.exit(-3);
                }
            }
            try(BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()))) {
                StringBuilder data = new StringBuilder();
                String line = "";
                while((line = reader.readLine()) != null) {
                    data.append(line).append("\n");
                }
                Pattern p = Pattern.compile("(?<=\"nextPageToken\": \")[^\"]+");
                Matcher m = p.matcher(data);
                if(m.find()) {
                    writer.write(data.append(",").toString());
                    writer.flush();
                    return m.group();
                }else {
                    writer.write(data.toString());
                    writer.flush();
                }
            }catch(IOException e) {
                e.printStackTrace();
            }
        }catch(IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
