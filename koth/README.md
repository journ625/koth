# FluxKoth

KOTH / Capture Point plugin — Minecraft 1.18.2, Purpur, Java 17.

## Build

```
mvn clean package
```

A kész jar a `target/FluxKoth-1.0.0.jar` helyen jön létre. Tedd a szerver `plugins`
mappájába. Ajánlott (nem kötelező) a **PlaceholderAPI** telepítése is, hogy a
placeholderek működjenek.

> Megjegyzés: ez a sandbox nem rendelkezik internet-hozzáféréssel, ezért a projekt
> nem lett ebben a környezetben lefordítva/tesztelve. A forráskód Purpur/Spigot
> 1.18.2 API-t és Java 17 szintaxist használ; build előtt győződj meg róla, hogy
> a `purpur-api:1.18.2-R0.1-SNAPSHOT` és a `placeholderapi:2.11.3` elérhető a
> megadott repository-kból (helyi Maven cache-t érdemes tisztítani, ha hibát dob).

## Alap használat

1. `/koth create <name>` — létrehoz egy új koth-ot
2. `/koth select <name>` — inspector módba lépsz + kapsz egy Foglaló Baltát
3. Bal klikk egy blokkra = 1. pont, jobb klikk = 2. pont (WorldEdit-szerű kijelölés)
4. `/koth setmode <name> <TIME|POINTS>` — mód beállítása
   - **TIME**: aki megszakítás nélkül bent marad a `settime`-mal megadott ideig, nyer
   - **POINTS**: a `setduration`-nal megadott ideig fut az esemény, a bent lévő
     (egyedüli) játékos másodpercenként pontot kap, a végén a legtöbb pontos nyer
5. `/koth settime <name> <mp>` — TIME mód: szükséges folyamatos foglalási idő
6. `/koth setduration <name> <mp>` — esemény max hossza / POINTS mód hossza
7. `/koth addreward item <name>` — kézben tartott tárgy hozzáadása jutalomként
8. `/koth addreward command <name> <parancs>` — parancs jutalom (`%player%`, `%koth%` támogatott)
9. `/koth enable <name>` — engedélyezi (csak engedélyezett + kész koth indítható)
10. `/koth start <name> [mp]` — manuális indítás; az opcionális `[mp]` felülírja csak erre az egy indításra a szükséges foglalási időt (TIME módban a folyamatos foglalási idő, POINTS módban az esemény hossza) — a koth alapértelmezett `settime`/`setduration` értéke nem változik
11. `/koth stop <name>` — manuális leállítás
12. `/koth schedule add <name> <HH:mm> [mp]` — automatikus napi indítás adott időpontban, opcionálisan saját, csak erre az indításra érvényes idővel
13. `/koth list` — összes koth listázása
14. `/koth reload` — config.yml + messages.yml újratöltése

A plugin **korlátlan számú koth-ot** támogat (100-200+ is), mivel minden koth a
`config.yml`-ben, a `koths.<név>` szekció alatt van tárolva, saját néven azonosítva.

## Chat spam elkerülése

Alapból a "foglalja..." (`Koth.Camping`) üzenet **nem másodpercenként**, hanem
`config.yml` → `settings.camping-message-interval-seconds` (alapból **25 másodperc**)
gyakorisággal íródik ki, hogy ne spammelje tele a chatet. A `Koth.StartedCamping`
(foglalás megkezdése) és a `Koth.LostControl` (megszakadt a foglalás) üzenetek
mindig azonnal kiíródnak, ahogy történnek.

## BossBar

Amíg egy koth aktív, minden online játékos lát egy BossBar-t, ami mutatja:
- a koth nevét
- a hátralévő időt (pl. `7:00` → `6:59` ahogy telik a foglalás)
- **TIME módban**: ki foglalja épp
- **POINTS módban**: az első helyezett játékos neve + pontszáma, és ki foglalja épp

A progress csík az eltelt/hátralévő idő arányában töltődik.

## Üzenetek (messages.yml)

Minden chatre kiírt szöveg testreszabható a `messages.yml`-ben, `enabled: true/false`
kapcsolóval soronként ki/be kapcsolható. Elérhető változók üzenetenként:
`%prefix% %player% %koth% %time% %uptime% %args%`

## Placeholderek (PlaceholderAPI)

Globális:
- `%fluxkoth_active_amount%` — aktív koth-ok száma
- `%fluxkoth_total_amount%` — összes koth száma
- `%fluxkoth_active_list%` — aktív koth-ok neve vesszővel elválasztva

Koth-specifikus (`<koth>` = a koth neve):
- `%fluxkoth_<koth>_status%` — Aktív / Inaktív
- `%fluxkoth_<koth>_enabled%` — true / false
- `%fluxkoth_<koth>_mode%` — TIME / POINTS
- `%fluxkoth_<koth>_world%`
- `%fluxkoth_<koth>_capturetime%` — beállított capture idő (mm:ss)
- `%fluxkoth_<koth>_duration%` — beállított esemény hossz (mm:ss)
- `%fluxkoth_<koth>_timeleft%` — hátralévő idő (mm:ss)
- `%fluxkoth_<koth>_camper%` — ki foglalja épp
- `%fluxkoth_<koth>_captureseconds%` — hány mp-e foglalja folyamatosan (TIME)
- `%fluxkoth_<koth>_topplayer%` — vezető játékos (POINTS)
- `%fluxkoth_<koth>_topplayer_points%` — vezető pontszáma (POINTS)
- `%fluxkoth_<koth>_uptime%` — mióta fut az esemény
- `%fluxkoth_<koth>_progress%` — 0-100 közti progress százalék
- `%fluxkoth_<koth>_schedules_amount%` — ütemezések száma

Guild:
- `%flux_guild_player%` — jelenleg csak stub (üres string), a te guild pluginodhoz
  kell hozzákötni a `FluxGuildPlaceholder` osztályban (lásd a benne lévő TODO-t).

## Jogosultság

`fluxkoth.admin` — az összes `/koth` parancshoz szükséges (alapból op-oknak van).
