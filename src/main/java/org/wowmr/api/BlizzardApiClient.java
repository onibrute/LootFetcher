package org.wowmr.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.wowmr.model.Encounter;
import org.wowmr.model.Instance;
import org.wowmr.model.LootItem;
import org.wowmr.model.ZoneOrInstance;

import java.net.URI;
import java.net.http.*;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

public class BlizzardApiClient {
    private static final String CLIENT_ID     = System.getenv("BLIZZARD_CLIENT_ID");
    private static final String CLIENT_SECRET = System.getenv("BLIZZARD_CLIENT_SECRET");
    private static final String REGION        = "eu";
    private static final String BASE_URL      = "https://" + REGION + ".api.blizzard.com";
    private static final String NS_STATIC     = "static-" + REGION;
    private static final String LOCALE        = "en_GB";

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
    private final ObjectMapper json = new ObjectMapper();
    private String token = null;

    private void authenticate() throws Exception {
        if (token != null) return;

        String authUrl = "https://" + REGION + ".battle.net/oauth/token";
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(authUrl))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic " +
                        java.util.Base64.getEncoder().encodeToString((CLIENT_ID + ":" + CLIENT_SECRET).getBytes()))
                .POST(HttpRequest.BodyPublishers.ofString("grant_type=client_credentials"))
                .build();

        HttpResponse<String> resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        JsonNode n = json.readTree(resp.body());
        token = n.path("access_token").asText(null);
        if (token == null) throw new IllegalStateException("No access_token in OAuth response");
    }

    private HttpRequest.Builder baseReq(String url) throws Exception {
        authenticate();
        return HttpRequest.newBuilder()
                .uri(URI.create(url))
                .header("Authorization", "Bearer " + token)
                .header("Accept", "application/json");
    }

    public List<Instance> fetchInstances() throws Exception {
        String url = BASE_URL + "/data/wow/journal-instance/index"
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode arr = json.readTree(resp.body()).path("instances");
        List<Instance> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode e : arr) {
                int id = e.path("id").asInt();
                String name = e.path("name").asText("Instance " + id);
                out.add(new Instance(id, name));
            }
        }
        return out;
    }

    public List<ZoneOrInstance> fetchZones() throws Exception {
        String url = BASE_URL + "/data/wow/zone/index"
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode arr = json.readTree(resp.body()).path("zones");
        List<ZoneOrInstance> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode e : arr) {
                int id = e.path("id").asInt();
                String name = e.path("name").asText("Zone " + id);
                out.add(new ZoneOrInstance(id, name, ZoneOrInstance.Type.ZONE));
            }
        }
        return out;
    }

    public Instance fetchInstanceDetails(int id) throws Exception {
        String url = BASE_URL + "/data/wow/journal-instance/" + id
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode root = json.readTree(resp.body());
        String name = root.path("name").asText();
        String desc = root.path("description").asText();
        String map = root.path("map").path("name").asText();

        String image = fetchInstanceImage(id);

        return new Instance(id, name, desc, map, image);
    }

    public List<Encounter> fetchEncounters(int instanceId) throws Exception {
        String url = BASE_URL + "/data/wow/journal-instance/" + instanceId
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode arr = json.readTree(resp.body()).path("encounters");
        List<Encounter> out = new ArrayList<>();
        if (arr.isArray()) {
            for (JsonNode e : arr) {
                int id = e.path("id").asInt();
                String name = e.path("name").asText("Boss " + id);
                out.add(new Encounter(id, name));
            }
        }
        return out;
    }

    public List<LootItem> fetchLootFromInstance(int instanceId, int encounterId) throws Exception {
        String url = BASE_URL + "/data/wow/journal-instance/" + instanceId
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode sections = json.readTree(resp.body()).path("sections");
        List<LootItem> out = new ArrayList<>();
        if (sections.isArray()) {
            for (JsonNode sec : sections) {
                JsonNode enc = sec.path("encounter");
                if (enc.path("id").asInt() == encounterId) {
                    for (JsonNode it : sec.path("items")) {
                        int id = it.path("id").asInt();
                        String name = it.path("name").asText("Item " + id);
                        out.add(new LootItem(id, name, 1, 0.0));
                    }
                }
            }
        }
        return out;
    }

    public LootItem fetchItem(int itemId) throws Exception {
        String url = BASE_URL + "/data/wow/item/" + itemId
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        if (resp.statusCode() != 200) {
            return new LootItem(itemId, "Unknown Item", 1, 0.0);
        }
        JsonNode root = json.readTree(resp.body());
        String name = root.path("name").asText("Item " + itemId);
        return new LootItem(itemId, name, 1, 0.0);
    }

    public String fetchInstanceImage(int instanceId) throws Exception {
        String url = BASE_URL + "/data/wow/media/journal-instance/" + instanceId
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode assets = json.readTree(resp.body()).path("assets");
        for (JsonNode a : assets) {
            System.out.println("  >> asset: " + a);
            String key = a.path("key").asText();
            if ("main".equals(key) || "tile".equals(key) || "icon".equals(key)) {
                return a.path("value").asText(null);
            }
        }
        return null;
    }

    public String fetchEncounterImage(int encounterId) throws Exception {
        String url = BASE_URL + "/data/wow/media/journal-encounter/" + encounterId
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode assets = json.readTree(resp.body()).path("assets");
        for (JsonNode a : assets) {
            System.out.println("  >> asset: " + a);
            String key = a.path("key").asText();
            if ("main".equals(key) || "icon".equals(key)) {
                return a.path("value").asText(null);
            }
        }
        return null;
    }

    public String fetchItemMedia(int itemId) throws Exception {
        String url = BASE_URL + "/data/wow/media/item/" + itemId
                + "?namespace=" + NS_STATIC + "&locale=" + LOCALE;
        HttpResponse<String> resp = http.send(baseReq(url).GET().build(), HttpResponse.BodyHandlers.ofString());

        JsonNode assets = json.readTree(resp.body()).path("assets");
        for (JsonNode a : assets) {
            System.out.println("  >> asset: " + a);
            String key = a.path("key").asText();
            if ("icon".equals(key)) {
                return a.path("value").asText(null);
            }
        }
        return null;
    }
}
