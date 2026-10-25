"""Writes en_us.json and ru_ru.json. Edit the text here, then run: python3 tools/gen_lang.py"""
import json
import os

OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "cornerofeye", "lang")

# Sign messages: one entry per sign, one string per line (Signs.LINES must match the line counts).
SIGNS = {
    "en_us": [
        ["I SEE YOU"],
        ["DON'T", "LOOK", "AT ME"],
        ["it was here", "before you"],
        ["WHY DID YOU", "COME BACK"],
        ["leave."],
        ["it is", "behind you"],
        ["the light", "won't last", "forever"],
        ["I watched", "you sleep"],
        ["count your", "torches"],
        ["only from", "the corner", "of your eye"],
    ],
    "ru_ru": [
        ["Я ВИЖУ ТЕБЯ"],
        ["НЕ", "СМОТРИ", "НА МЕНЯ"],
        ["оно было здесь", "до тебя"],
        ["ЗАЧЕМ ТЫ", "ВЕРНУЛСЯ"],
        ["уходи."],
        ["оно", "позади тебя"],
        ["свет", "не", "вечен"],
        ["я смотрел,", "как ты спишь"],
        ["пересчитай", "факелы"],
        ["только", "краем", "глаза"],
    ],
}

# Written in blood on walls (Marks.MESSAGES must match the count). It doesn't think like a person, and it
# doesn't quite know how words work; WritingRenderer mangles the letters further.
WRITINGS = {
    "en_us": ["WARM WARM WARM YOURS", "you have too few fingers", "i tried on your voice", "you sleep loudly",
              "COUNTING YOUR RIBS 1 2 3 4", "dont blink. i will blink for you", "it smells of you on the inside",
              "open your mouth wider than you can", "WE WE WE WE WE WE", "i stand in the corner every night",
              "they stopped screaming. so will you", "skin is a door", "i am learning to talk like you",
              "why do you close your eyes when i am near", "LIGHT MORE. I LIKE HOW THEY GO OUT",
              "you look good from the corner"],
    "ru_ru": ["ТЕПЛО ТЕПЛО ТЕПЛО ТВОЁ", "у тебя слишком мало пальцев", "я примерял твой голос", "ты спишь громко",
              "СЧИТАЮ ТВОИ РЁБРА 1 2 3 4", "не моргай. я моргну за тебя", "здесь пахнет тобой изнутри",
              "открой рот шире чем можешь", "МЫ МЫ МЫ МЫ МЫ МЫ", "я стою в углу каждую ночь",
              "они перестали кричать. ты тоже перестанешь", "кожа это дверь", "я учусь говорить как ты",
              "почему ты закрываешь глаза когда я рядом", "ЗАЖГИ ЕЩЁ. МНЕ НРАВИТСЯ КАК ОНИ ГАСНУТ",
              "ты хорошо смотришься из угла"],
}

# Menu corruption (MenuHaunt): title splashes, buttons that say the wrong thing for a moment, tooltip whispers.
SPLASHES = {
    "en_us": ["It remembers this world!", "Don't turn around!", "Did you leave the light on?", "It's already here!",
              "Look in the corner!", "We missed you!", "Don't load that world!", "How many nights are left?",
              "From the corner of your eye!", "It waits in the menu too!"],
    "ru_ru": ["Оно помнит этот мир!", "Не оборачивайся!", "Ты не выключил свет?", "Оно уже здесь!",
              "Посмотри в угол!", "Мы скучали!", "Не загружай этот мир!", "Сколько ночей осталось?",
              "Краем глаза!", "Оно ждёт и в меню!"],
}
GLITCHES = {
    "en_us": ["It is waiting", "Don't leave", "Go back to it", "You are not alone", "Behind you", "Don't look",
              "It is here", "Stay"],
    "ru_ru": ["Оно ждёт", "Не уходи", "Вернись к нему", "Ты не один", "Позади тебя", "Не смотри", "Оно здесь",
              "Останься"],
}
TOOLTIPS = {
    "en_us": {
        "any": ["it touched this in the night", "warm", "whose is this?", "put it down. put it down. put it down.",
                "this won't help", "it can see this too", "smells of you", "did you count them?"],
        "torch": ["fewer than yesterday", "it doesn't like it when there are many", "this one will go out too"],
        "lantern": ["it knows about this", "shine it in my face", "don't waste it"],
    },
    "ru_ru": {
        "any": ["оно трогало это ночью", "тёплое", "чьё это?", "положи. положи. положи.",
                "это не поможет", "оно тоже это видит", "пахнет тобой", "ты пересчитывал?"],
        "torch": ["их меньше, чем вчера", "оно не любит, когда их много", "этот тоже погаснет"],
        "lantern": ["оно знает про него", "посвети мне в лицо", "не трать зря"],
    },
}

# What the mimic says in chat, as whoever it is copying (MimicEntity.SAID_LINES).
MIMIC_SAID = {
    "en_us": ["over here", "come here. here. here.", "i found something, come and look", "it's safe here",
              "why are you so far away", "don't be scared"],
    "ru_ru": ["я тут", "иди сюда. сюда. сюда.", "я кое-что нашёл, иди посмотри", "здесь безопасно",
              "почему ты так далеко", "не бойся"],
}

WHISPERS = {
    "en_us": ["i see you", "don't look at me", "your torches are going out", "it's so dark here",
              "why are you still here", "you are not alone", "i was here before you", "one more night",
              "turn around. no, don't", "i'm in the corner", "count the nights", "let me in"],
    "ru_ru": ["я вижу тебя", "не смотри на меня", "твои факелы гаснут", "здесь так темно",
              "почему ты всё ещё здесь", "ты не один", "я был здесь до тебя", "ещё одна ночь",
              "обернись. нет, не надо", "я в углу", "считай ночи", "впусти меня"],
}

SAID = {
    "en_us": ["help", "who's there?", "i can't see anything", "it's in the corner", "turn off the lights",
              "come here, i want to show you something"],
    "ru_ru": ["помогите", "кто здесь?", "я ничего не вижу", "оно в углу", "выключи свет",
              "иди сюда, я тебе кое-что покажу"],
}

SLEEP = {
    "en_us": ["Wake up.", "Something sat down on the edge of your bed.", "You are not alone."],
    "ru_ru": ["Проснись.", "Кто-то сел на край твоей кровати.", "Ты здесь не один."],
}

# The previous owner's notes, by chapter. Chapters turn up one at a time (see Journal.java).
JOURNAL = {
    "en_us": {
        "arrival": [
            "If you are reading this, it found you too.\n\nI don't know how many nights I have left. I'll write down what I understand, when I understand it.",
            "It stands in the dark, right at the edge of your vision.\n\nTurn to look and there is nothing there. You can only ever see it from the corner of your eye.",
            "It puts out the lights. Only the ones you aren't looking at. Torches, furnaces, lanterns, even glowstone.\n\nNothing it puts out comes back on by itself.",
        ],
        "scraps": [
            "I went to where I had seen it standing. It didn't let me close. But there was something on the ground: a scrap, cold, like a torn piece of shadow.",
            "I think they matter. Collect them while it is still only watching.\n\nLater it leaves almost nothing.",
            "The dog growled at an empty corner all night. In the morning there was a scrap lying there.\n\nAnimals know where it is. Look where they look.",
        ],
        "lantern": [
            "Light doesn't kill it. It shows it.\n\nA lantern in the middle, a scrap on each side, amethyst in the corners. I made one. It hums.",
        ],
        "flash": [
            "Hold the lantern until it chimes, then let go.\n\nIf it is anywhere in sight at that moment, even at the very edge, it is caught.",
            "Every flash burns a scrap. Don't waste them.\n\nOnce the hunt begins you have to be much closer to catch it.",
        ],
        "moments": [
            "It has started coming differently. Sometimes it shows itself. A clock starts ticking in your head and you just know what you have to do.\n\nDo it.",
            "Flint lights what it put out again, but never while it is still near.\n\nOnly fresh light saves you, and it has to be bright: stand right next to it.",
            "A bell on a string: a gold ingot with iron nuggets around it. Keep it at hand. It rings when it is near.\n\nNear the end it rang when nothing was there. I think.",
            "Glass in an iron frame, on a stick. In a mirror you can look straight at it. Only at what is behind you.\n\nEvery time it is in there, the glass cracks.",
            "Paper, gunpowder, redstone, coal: a flare. Throw it and it burns red for half a minute. That fire it cannot put out.\n\nDon't make the doll. Wool, string and a scrap. I did. It is still here.",
        ],
        "three": [
            "It screams when you catch it.\n\nThree times. Catch it three times and it leaves this world for good.",
            "The last time doesn't count just any night. Only under a blood moon.\n\nThat night it doesn't come alone. The others are empty. The dog only growls at the real one.",
        ],
        "hunt": [
            "The seventh night. It has stopped waiting. It comes through walls now.\n\nIf you don't have the lantern yet, find scraps while you still can.",
            "I caught it once.\n\nIt's in the corner now. I can see it.\n\nDon't look at it.",
        ],
    },
    "ru_ru": {
        "arrival": [
            "Если ты это читаешь, оно нашло и тебя.\n\nНе знаю, сколько ночей у меня осталось. Буду записывать, что пойму, когда пойму.",
            "Оно стоит в темноте, на самом краю зрения.\n\nПовернёшься посмотреть — там никого. Его можно увидеть только краем глаза.",
            "Оно гасит свет. Только тот, на который не смотришь. Факелы, печи, фонари, даже светокамень.\n\nСамо ничего не загорается.",
        ],
        "scraps": [
            "Я пошёл туда, где видел его. Близко оно не подпустило. Но на земле что-то осталось: обрывок, холодный, как клочок тени.",
            "Кажется, они важны. Собирай их, пока оно только смотрит.\n\nПотом оно почти ничего не оставляет.",
            "Пёс всю ночь рычал в пустой угол. Утром там лежал обрывок.\n\nЖивотные чувствуют, где оно. Смотри туда, куда смотрят они.",
        ],
        "lantern": [
            "Свет его не убивает. Он его показывает.\n\nФонарь в середине, обрывки по сторонам, аметист по углам. Я сделал такой. Он гудит.",
        ],
        "flash": [
            "Держи фонарь, пока не зазвенит, и отпусти.\n\nЕсли в этот миг оно хоть где-то в поле зрения, даже на самом краю, — оно попалось.",
            "Каждая вспышка сжигает обрывок. Не трать зря.\n\nКогда начнётся охота, ловить его придётся с куда меньшего расстояния.",
        ],
        "moments": [
            "Оно стало приходить по-другому. Иногда показывается само. В голове начинают тикать часы, и ты просто знаешь, что надо сделать.\n\nДелай.",
            "Огниво зажигает погашенное им, но никогда — пока оно рядом.\n\nСпасает только свежий свет, и он должен быть ярким: стой прямо возле него.",
            "Колокольчик на нитке: золотой слиток в железных самородках. Держи его под рукой. Он звенит, когда оно рядом.\n\nПод конец он звенел и тогда, когда никого не было. Наверное.",
            "Стекло в железной оправе, на палке. В зеркале на него можно смотреть прямо. Только на то, что за спиной.\n\nКаждый раз, когда оно там, стекло трескается.",
            "Бумага, порох, редстоун, уголь — сигнальный огонь. Брось, и он полминуты горит красным. Этот огонь оно не потушит.\n\nНе делай куклу. Шерсть, нитка и обрывок. Я сделал. Она всё ещё здесь.",
        ],
        "three": [
            "Когда его ловишь, оно кричит.\n\nТри раза. Поймай его трижды, и оно уйдёт из этого мира навсегда.",
            "Последний раз засчитывается не в любую ночь. Только под кровавой луной.\n\nВ ту ночь оно приходит не одно. Другие пустые. Собака рычит только на настоящее.",
        ],
        "hunt": [
            "Седьмая ночь. Оно больше не ждёт. Теперь оно проходит сквозь стены.\n\nЕсли у тебя ещё нет фонаря, ищи обрывки, пока можешь.",
            "Я поймал его один раз.\n\nОно сейчас в углу. Я его вижу.\n\nНе смотри на него.",
        ],
    },
}

# The notebooks in the chest at a victim's camp (VictimCampFeature.DIARIES x PAGES), each ending mid-word.
CAMP_DIARIES = {
    "en_us": [
        [
            "Day 3.\n\nLight helps. It won't come near while the torches burn. So I will keep torches burning. Lots of torches.",
            "Day 5.\n\nThe torches go out by themselves. They don't burn down, they go out, as if someone pinched the flame. I saw it at the edge of the light. Only from the corner of my eye.",
            "Day 6.\n\nLook straight at it and it isn't there. I stopped looking straight at it.\n\nThat is worse.",
            "Day 7.\n\nIt leaves scraps behind. Black, cold, like a shadow you can hold. I am collecting them. I think a lantern could be made from them.",
            "It is breathing behind the tent.\n\nI will not look. I will not look. I will not lo",
        ],
        [
            "We came here together, the two of us.\n\nI am writing this so I remember there were two of us.",
            "He said he hears footsteps behind him. One step behind his own. I told him it was an echo.\n\nThere is nothing here to echo.",
            "In the morning his bedroll was empty. Handprints led away from it into the dark.\n\nLong fingers. Too long.",
            "Don't sleep where you slept yesterday.\n\nIt remembers.",
            "I woke up and couldn't move. It was standing at my feet, leaning over me. Slowly.\n\nI still can't",
        ],
        [
            "Don't build high.\n\nI thought it couldn't reach me on the tower.",
            "It didn't climb. It stood below and looked up.\n\nAll night.\n\nWithout blinking.",
            "I looked away for a second.",
            "If you find this: light. A flash, right at it. Catch it in the light three times.\n\nThe last time only under a red moon. Before that it doesn't count.",
            "It is already be",
        ],
    ],
    "ru_ru": [
        [
            "День 3.\n\nСвет помогает. Пока горят факелы, оно не подходит. Значит, буду жечь факелы. Много факелов.",
            "День 5.\n\nФакелы гаснут сами. Не догорают, а гаснут, будто кто-то зажимает пламя пальцами. Я видел его у края света. Только краем глаза.",
            "День 6.\n\nЕсли смотреть прямо, его нет. Я перестал смотреть прямо.\n\nТак хуже.",
            "День 7.\n\nОно оставляет обрывки. Чёрные, холодные, как тень, которую можно взять в руки. Я собираю их. Мне кажется, из них можно сделать фонарь.",
            "Оно дышит за палаткой.\n\nЯ не буду смотреть. Я не буду смотреть. Я не бу",
        ],
        [
            "Мы пришли сюда вдвоём.\n\nПишу это, чтобы помнить, что нас было двое.",
            "Он сказал, что слышит за спиной шаги. На шаг позже своих. Я сказал, что это эхо.\n\nЗдесь нечему отражаться.",
            "Утром его лежанка была пустой. От неё в темноту вели отпечатки рук.\n\nДлинные пальцы. Слишком длинные.",
            "Не спи там, где спал вчера.\n\nОно запоминает.",
            "Проснулся и не мог пошевелиться. Оно стояло в ногах и наклонялось ко мне. Медленно.\n\nЯ и сейчас не",
        ],
        [
            "Не строй высоко.\n\nЯ думал, на башне оно меня не достанет.",
            "Оно не полезло. Оно стояло внизу и смотрело вверх.\n\nВсю ночь.\n\nНе моргая.",
            "Я отвернулся на секунду.",
            "Если найдёшь это: свет. Вспышкой, прямо в него. Поймать его в свет три раза.\n\nПоследний раз — только под красной луной. Раньше не считается.",
            "Оно уже за",
        ],
    ],
}

TEXT = {
    "en_us": {
        "entity.cornerofeye.the_one": "The One in the Corner",
        "item.cornerofeye.the_one_spawn_egg": "The One in the Corner Spawn Egg",
        "item.cornerofeye.shadow_scrap": "Shadow Scrap",
        "item.cornerofeye.shadow_scrap.tooltip": "Cold to the touch. It was standing right here.",
        "item.cornerofeye.warding_bell": "Warding Bell",
        "item.cornerofeye.warding_bell.tooltip": "Rings when it is near. Keep it in the hotbar or off hand.",
        "item.cornerofeye.hand_mirror": "Hand Mirror",
        "item.cornerofeye.hand_mirror.tooltip": "Hold right-click to see what is behind you.",
        "item.cornerofeye.flare": "Flare",
        "item.cornerofeye.flare.tooltip": "Throw it. Burns red for half a minute, and that fire it cannot put out.",
        "item.cornerofeye.cursed_doll": "Cursed Doll",
        "item.cornerofeye.cursed_doll.tooltip": "She is looking at you.",
        "entity.cornerofeye.flare": "Flare",
        "entity.cornerofeye.mimic": "Someone",
        "cornerofeye.challenge.find_bed": "FIND YOUR BED",
        "cornerofeye.challenge.find_bed.hint": "This is a dream. Lie down in your bed and you will wake up.",
        "cornerofeye.message.dream_start": "You don't remember getting out of bed.",
        "cornerofeye.message.dream_won": "You lie down and close your eyes. When you open them it is morning, and there is something in your hand.",
        "cornerofeye.message.dream_lost": "You wake up screaming.",
        "cornerofeye.message.dream_lantern": "The lantern won't light. This is a dream.",
        "cornerofeye.night.finale": "The last night.",
        "cornerofeye.night.victory": "THE CORNERS ARE EMPTY",
        "cornerofeye.night.victory.sub": "It is gone. For good.",
        "cornerofeye.message.not_tonight": "It laughs. The last time only counts under a blood moon.",
        "cornerofeye.message.decoy": "Empty. There was nothing there.",
        "cornerofeye.message.returned": "The corners are not empty any more.",
        "commands.cornerofeye.dream": "%s is dreaming",
        "commands.cornerofeye.wounds": "Wounds set to %s",
        "commands.cornerofeye.dream.failed": "No dream here: nightmares are off, or no room for a bed",
        "subtitles.cornerofeye.warding_bell": "A little bell rings",
        "subtitles.cornerofeye.claw_scrape": "Claws scrape inside the wall",
        "cornerofeye.message.doll_back": "The doll is back in your pocket.",
        "cornerofeye.message.doll_crumbled": "The old doll falls apart in your hands. It only wants one of you.",
        "cornerofeye.message.doll_burned": "Somewhere, a doll burned. It is quieter now.",
        "item.cornerofeye.lantern_of_revelation": "Lantern of Revelation",
        "item.cornerofeye.lantern_of_revelation.tooltip.0": "Hold to charge, release to flash.",
        "item.cornerofeye.lantern_of_revelation.tooltip.1": "Each flash burns a Shadow Scrap.",
        "item.cornerofeye.lantern_of_revelation.no_fuel": "It needs a Shadow Scrap to burn.",

        "subtitles.cornerofeye.the_one_breath": "Something breathes",
        "subtitles.cornerofeye.the_one_skitter": "Bones click",
        "subtitles.cornerofeye.the_one_scream": "Shriek",
        "subtitles.cornerofeye.the_one_wail": "Something wails",
        "subtitles.cornerofeye.the_one_vanish": "Something vanishes",
        "subtitles.cornerofeye.the_one_banish": "Something comes apart",
        "subtitles.cornerofeye.whisper": "Whispering",
        "subtitles.cornerofeye.heartbeat": "Your heart pounds",
        "subtitles.cornerofeye.drone": "Low hum",
        "subtitles.cornerofeye.distant": "Distant noise",
        "subtitles.cornerofeye.lantern_charge": "Lantern hums",
        "subtitles.cornerofeye.lantern_flash": "Lantern flashes",
        "subtitles.cornerofeye.glimpse": "Something at the edge of sight",
        "subtitles.cornerofeye.player_breath": "You breathe faster",
        "subtitles.cornerofeye.tinnitus": "Ringing in your ears",
        "subtitles.cornerofeye.timer_tick": "Clock ticks",
        "subtitles.cornerofeye.night_bell": "A bell, far away",
        "subtitles.cornerofeye.crack": "Something cracks",
        "cornerofeye.night.title": "NIGHT %s",
        "cornerofeye.night.sub.1": "You are not alone here.",
        "cornerofeye.night.sub.2": "Something has noticed you.",
        "cornerofeye.night.sub.3": "It is watching.",
        "cornerofeye.night.sub.4": "It is coming closer.",
        "cornerofeye.night.sub.5": "It knows where you sleep.",
        "cornerofeye.night.sub.6": "The last quiet night.",
        "cornerofeye.night.sub.7": "It has stopped waiting.",
        "cornerofeye.night.sub.late": "It is hungry.",
        "cornerofeye.night.blood": "BLOOD MOON",
        "commands.cornerofeye.bloodmoon": "Blood moon: %s",

        "cornerofeye.challenge.light_run": "GET INTO THE LIGHT",
        "cornerofeye.challenge.light_run.hint": "Place a torch or run to one",
        "cornerofeye.challenge.light_place": "LIGHT IT UP",
        "cornerofeye.challenge.light_place.hint": "Place a torch. Now.",
        "cornerofeye.challenge.dont_look": "DON'T TURN AROUND",
        "cornerofeye.challenge.dont_look.hint": "Whatever you hear",
        "cornerofeye.challenge.leave_water": "GET OUT OF THE WATER",
        "cornerofeye.challenge.leave_water.hint": "Shore or a boat. Now.",
        "cornerofeye.message.massacre_done": "The village has gone very quiet.",
        "cornerofeye.message.cant_relight": "It won't catch. Not while it's still here.",
        "cornerofeye.challenge.light_meter": "LIGHT %s / %s",
        "block.cornerofeye.unlit_torch": "Burnt-out Torch",
        "block.cornerofeye.unlit_wall_torch": "Burnt-out Torch",
        "entity.cornerofeye.corpse": "Corpse",
        "block.cornerofeye.blood": "Blood",
        "block.cornerofeye.mark": "Marks",
        "block.cornerofeye.writing": "Writing",
        "cornerofeye.challenge.still": "DON'T MOVE",
        "cornerofeye.challenge.still.hint": "Crouch and freeze. It finds you by sound.",
        "cornerofeye.message.survived": "It's gone. For now.",
        "commands.cornerofeye.moment": "Started %s for %s",
        "commands.cornerofeye.moment.failed": "Can't set up %s here",
        "advancements.cornerofeye.survived.title": "Not Tonight",
        "advancements.cornerofeye.survived.description": "Live through one of its moments",

        "cornerofeye.stage.0": "Silence",
        "cornerofeye.stage.1": "Watching",
        "cornerofeye.stage.2": "Approaching",
        "cornerofeye.stage.3": "Hunting",
        "cornerofeye.journal.title": "Someone's Notes, part %s",
        "cornerofeye.message.journal": "There's a notebook in your pocket. You don't remember picking it up.",
        "cornerofeye.message.page": "Another page has turned up among your things.",
        "cornerofeye.message.stage.2": "It's getting closer.",
        "cornerofeye.message.stage.3": "It has stopped waiting.",
        "cornerofeye.message.wounded": "It screams. %s more.",
        "cornerofeye.message.final_wound": "It's coming apart.",
        "cornerofeye.message.banished": "The corners are empty now.",

        "commands.cornerofeye.status": "Stage: %s · day %s · wounds: %s · banished: %s · here now: %s",
        "commands.cornerofeye.stage.auto": "Stage follows the days again (now: %s)",
        "commands.cornerofeye.stage.set": "Stage set to %s",
        "commands.cornerofeye.unknown": "Unknown: %s",
        "commands.cornerofeye.appear": "It came for %s",
        "commands.cornerofeye.appear.failed": "No spot dark enough nearby",
        "commands.cornerofeye.event": "Ran %s for %s",
        "commands.cornerofeye.event.failed": "%s can't happen here right now",
        "commands.cornerofeye.reset": "Everything reset to day 0",

        "advancements.cornerofeye.root.title": "Corner of the Eye",
        "advancements.cornerofeye.root.description": "Something lives where your eyes don't reach",
        "advancements.cornerofeye.touched.title": "Cold Fingers",
        "advancements.cornerofeye.touched.description": "Let it reach you in the dark",
        "advancements.cornerofeye.shadow_scrap.title": "It Was Standing Right Here",
        "advancements.cornerofeye.shadow_scrap.description": "Pick up a Shadow Scrap where it stood",
        "advancements.cornerofeye.lantern.title": "Seen As It Is",
        "advancements.cornerofeye.lantern.description": "Make a Lantern of Revelation",
        "advancements.cornerofeye.caught.title": "Caught in the Light",
        "advancements.cornerofeye.caught.description": "Catch it with a flash of the Lantern of Revelation",
        "advancements.cornerofeye.banished.title": "The Corners Are Empty",
        "advancements.cornerofeye.banished.description": "Drive it away for good",
        "subtitles.cornerofeye.claw_glass": "Claws scrape down the glass",
        "subtitles.cornerofeye.glass_tap": "Something taps on the glass",
        "commands.cornerofeye.visit": "It went through %s's home.",
        "commands.cornerofeye.visit.failed": "No bed to visit, or someone is home.",
        "commands.cornerofeye.morning": "It was at the wall by %s's bed.",
        "cornerofeye.learned.light": "It remembers your light. It will not wait for it to go out.",
        "cornerofeye.learned.height": "It remembers where you hide. Up high. It is in no hurry.",
        "cornerofeye.learned.bed": "It knows where you sleep.",
        "cornerofeye.learned.gaze": "It knows you are looking for it. It keeps behind you now.",
        "cornerofeye.camp.diary.title": "Someone's notes",
        "block.cornerofeye.bell_string": "Bell on a Thread",
        "block.cornerofeye.bell_string.tooltip": "Hang it in a doorway. It rings when it goes by.",
        "subtitles.cornerofeye.bell_string": "A bell on a thread rings",
        "cornerofeye.message.taken": "%s's voice broke off somewhere in the dark.",
        "cornerofeye.message.taken_back": "The light brought %s back.",
        "cornerofeye.message.dream_start_taken": "Something has hold of your ankle. The ground scrapes past under you. Then nothing.",
        "cornerofeye.message.dream_won_taken": "It lets go. You are lying on the ground where it dragged you, and there is something in your hand.",
        "subtitles.cornerofeye.flesh": "Flesh tears",
    },
    "ru_ru": {
        "entity.cornerofeye.the_one": "Тот, что в углу",
        "item.cornerofeye.the_one_spawn_egg": "Яйцо призыва Того, что в углу",
        "item.cornerofeye.shadow_scrap": "Обрывок тени",
        "item.cornerofeye.shadow_scrap.tooltip": "Холодный на ощупь. Оно стояло прямо здесь.",
        "item.cornerofeye.warding_bell": "Колокольчик-оберег",
        "item.cornerofeye.warding_bell.tooltip": "Звенит, когда оно рядом. Держи на панели быстрого доступа или во второй руке.",
        "item.cornerofeye.hand_mirror": "Зеркальце",
        "item.cornerofeye.hand_mirror.tooltip": "Зажми ПКМ, чтобы посмотреть, что за спиной.",
        "item.cornerofeye.flare": "Сигнальный огонь",
        "item.cornerofeye.flare.tooltip": "Брось. Полминуты горит красным, и этот огонь оно не потушит.",
        "item.cornerofeye.cursed_doll": "Проклятая кукла",
        "item.cornerofeye.cursed_doll.tooltip": "Она смотрит на тебя.",
        "entity.cornerofeye.flare": "Сигнальный огонь",
        "entity.cornerofeye.mimic": "Кто-то",
        "cornerofeye.challenge.find_bed": "НАЙДИ СВОЮ КРОВАТЬ",
        "cornerofeye.challenge.find_bed.hint": "Это сон. Ляг в свою кровать, и ты проснёшься.",
        "cornerofeye.message.dream_start": "Ты не помнишь, как встал с кровати.",
        "cornerofeye.message.dream_won": "Ты ложишься и закрываешь глаза. Когда открываешь, уже утро, и в руке что-то зажато.",
        "cornerofeye.message.dream_lost": "Ты просыпаешься с криком.",
        "cornerofeye.message.dream_lantern": "Фонарь не загорается. Это сон.",
        "cornerofeye.night.finale": "Последняя ночь.",
        "cornerofeye.night.victory": "УГЛЫ ОПУСТЕЛИ",
        "cornerofeye.night.victory.sub": "Оно ушло. Навсегда.",
        "cornerofeye.message.not_tonight": "Оно смеётся. Последний раз засчитывается только под кровавой луной.",
        "cornerofeye.message.decoy": "Пусто. Там ничего не было.",
        "cornerofeye.message.returned": "Углы снова не пусты.",
        "commands.cornerofeye.dream": "%s видит сон",
        "commands.cornerofeye.wounds": "Ранений теперь: %s",
        "commands.cornerofeye.dream.failed": "Сон не получится: кошмары выключены или негде поставить кровать",
        "subtitles.cornerofeye.warding_bell": "Звенит колокольчик",
        "subtitles.cornerofeye.claw_scrape": "Когти скребут в стене",
        "cornerofeye.message.doll_back": "Кукла снова у тебя в кармане.",
        "cornerofeye.message.doll_crumbled": "Старая кукла рассыпается в руках. Ей нужна только одна.",
        "cornerofeye.message.doll_burned": "Где-то сгорела кукла. Стало тише.",
        "item.cornerofeye.lantern_of_revelation": "Фонарь откровения",
        "item.cornerofeye.lantern_of_revelation.tooltip.0": "Удерживайте, чтобы зарядить, отпустите для вспышки.",
        "item.cornerofeye.lantern_of_revelation.tooltip.1": "Каждая вспышка сжигает обрывок тени.",
        "item.cornerofeye.lantern_of_revelation.no_fuel": "Нечему гореть: нужен обрывок тени.",

        "subtitles.cornerofeye.the_one_breath": "Что-то дышит",
        "subtitles.cornerofeye.the_one_skitter": "Щёлкают кости",
        "subtitles.cornerofeye.the_one_scream": "Визг",
        "subtitles.cornerofeye.the_one_wail": "Что-то воет",
        "subtitles.cornerofeye.the_one_vanish": "Что-то исчезает",
        "subtitles.cornerofeye.the_one_banish": "Что-то распадается",
        "subtitles.cornerofeye.whisper": "Шёпот",
        "subtitles.cornerofeye.heartbeat": "Стучит сердце",
        "subtitles.cornerofeye.drone": "Низкий гул",
        "subtitles.cornerofeye.distant": "Далёкий звук",
        "subtitles.cornerofeye.lantern_charge": "Фонарь гудит",
        "subtitles.cornerofeye.lantern_flash": "Вспышка фонаря",
        "subtitles.cornerofeye.glimpse": "Что-то на краю зрения",
        "subtitles.cornerofeye.player_breath": "Ты дышишь чаще",
        "subtitles.cornerofeye.tinnitus": "Звон в ушах",
        "subtitles.cornerofeye.timer_tick": "Тикают часы",
        "subtitles.cornerofeye.night_bell": "Далёкий колокол",
        "subtitles.cornerofeye.crack": "Что-то трескается",
        "cornerofeye.night.title": "НОЧЬ %s",
        "cornerofeye.night.sub.1": "Ты здесь не один.",
        "cornerofeye.night.sub.2": "Тебя заметили.",
        "cornerofeye.night.sub.3": "Оно смотрит.",
        "cornerofeye.night.sub.4": "Оно подходит ближе.",
        "cornerofeye.night.sub.5": "Оно знает, где ты спишь.",
        "cornerofeye.night.sub.6": "Последняя тихая ночь.",
        "cornerofeye.night.sub.7": "Оно больше не ждёт.",
        "cornerofeye.night.sub.late": "Оно голодно.",
        "cornerofeye.night.blood": "КРОВАВАЯ ЛУНА",
        "commands.cornerofeye.bloodmoon": "Кровавая луна: %s",

        "cornerofeye.challenge.light_run": "ВСТАНЬ НА СВЕТ",
        "cornerofeye.challenge.light_run.hint": "Поставь факел или беги к свету",
        "cornerofeye.challenge.light_place": "ЗАЖГИ СВЕТ",
        "cornerofeye.challenge.light_place.hint": "Поставь факел. Сейчас.",
        "cornerofeye.challenge.dont_look": "НЕ ОБОРАЧИВАЙСЯ",
        "cornerofeye.challenge.dont_look.hint": "Что бы ты ни услышал",
        "cornerofeye.challenge.leave_water": "ВЫБЕРИСЬ ИЗ ВОДЫ",
        "cornerofeye.challenge.leave_water.hint": "На берег или в лодку. Быстро.",
        "cornerofeye.message.massacre_done": "В деревне стало очень тихо.",
        "cornerofeye.message.cant_relight": "Не загорается. Пока оно здесь — нет.",
        "cornerofeye.challenge.light_meter": "СВЕТ %s / %s",
        "block.cornerofeye.unlit_torch": "Потухший факел",
        "block.cornerofeye.unlit_wall_torch": "Потухший факел",
        "entity.cornerofeye.corpse": "Труп",
        "block.cornerofeye.blood": "Кровь",
        "block.cornerofeye.mark": "Следы",
        "block.cornerofeye.writing": "Надпись",
        "cornerofeye.challenge.still": "НЕ ДВИГАЙСЯ",
        "cornerofeye.challenge.still.hint": "Присядь и замри. Оно ищет тебя по звуку.",
        "cornerofeye.message.survived": "Оно ушло. Пока что.",
        "commands.cornerofeye.moment": "Момент %s для %s",
        "commands.cornerofeye.moment.failed": "Здесь не получится: %s",
        "advancements.cornerofeye.survived.title": "Не сегодня",
        "advancements.cornerofeye.survived.description": "Переживите один из его моментов",

        "cornerofeye.stage.0": "Тишина",
        "cornerofeye.stage.1": "Наблюдение",
        "cornerofeye.stage.2": "Приближение",
        "cornerofeye.stage.3": "Охота",
        "cornerofeye.journal.title": "Чьи-то записи, часть %s",
        "cornerofeye.message.journal": "В кармане чужая записная книжка. Ты не помнишь, откуда она.",
        "cornerofeye.message.page": "Среди твоих вещей нашлась ещё одна страница.",
        "cornerofeye.message.stage.2": "Оно подходит ближе.",
        "cornerofeye.message.stage.3": "Оно больше не ждёт.",
        "cornerofeye.message.wounded": "Оно кричит. Осталось: %s.",
        "cornerofeye.message.final_wound": "Оно распадается.",
        "cornerofeye.message.banished": "Углы опустели.",

        "commands.cornerofeye.status": "Стадия: %s · день %s · ранений: %s · изгнано: %s · сейчас здесь: %s",
        "commands.cornerofeye.stage.auto": "Стадия снова зависит от дней (сейчас: %s)",
        "commands.cornerofeye.stage.set": "Стадия: %s",
        "commands.cornerofeye.unknown": "Неизвестно: %s",
        "commands.cornerofeye.appear": "Оно пришло за %s",
        "commands.cornerofeye.appear.failed": "Рядом нет достаточно тёмного места",
        "commands.cornerofeye.event": "Событие %s для %s",
        "commands.cornerofeye.event.failed": "Событие %s сейчас здесь невозможно",
        "commands.cornerofeye.reset": "Всё сброшено на день 0",

        "advancements.cornerofeye.root.title": "Краем глаза",
        "advancements.cornerofeye.root.description": "Что-то живёт там, куда не достаёт взгляд",
        "advancements.cornerofeye.touched.title": "Холодные пальцы",
        "advancements.cornerofeye.touched.description": "Позвольте ему дотянуться до вас в темноте",
        "advancements.cornerofeye.shadow_scrap.title": "Оно стояло прямо здесь",
        "advancements.cornerofeye.shadow_scrap.description": "Подберите обрывок тени там, где оно стояло",
        "advancements.cornerofeye.lantern.title": "Увидеть как есть",
        "advancements.cornerofeye.lantern.description": "Создайте Фонарь откровения",
        "advancements.cornerofeye.caught.title": "Пойман светом",
        "advancements.cornerofeye.caught.description": "Поймайте его вспышкой Фонаря откровения",
        "advancements.cornerofeye.banished.title": "Углы опустели",
        "advancements.cornerofeye.banished.description": "Изгоните его навсегда",
        "subtitles.cornerofeye.claw_glass": "Когти скребут по стеклу",
        "subtitles.cornerofeye.glass_tap": "Кто-то стучит в стекло",
        "commands.cornerofeye.visit": "Оно побывало дома у %s.",
        "commands.cornerofeye.visit.failed": "Нет кровати, или кто-то дома.",
        "commands.cornerofeye.morning": "Оно было у стены возле кровати %s.",
        "cornerofeye.learned.light": "Оно запомнило твой свет. Ждать, пока он погаснет, оно больше не будет.",
        "cornerofeye.learned.height": "Оно запомнило, где ты прячешься. Наверху. Оно не торопится.",
        "cornerofeye.learned.bed": "Оно знает, где ты спишь.",
        "cornerofeye.learned.gaze": "Оно знает, что ты высматриваешь его. Теперь оно держится за спиной.",
        "cornerofeye.camp.diary.title": "Чьи-то записи",
        "block.cornerofeye.bell_string": "Колокольчик на нитке",
        "block.cornerofeye.bell_string.tooltip": "Повесь в проёме. Зазвенит, когда оно пройдёт мимо.",
        "subtitles.cornerofeye.bell_string": "Звенит колокольчик на нитке",
        "cornerofeye.message.taken": "Голос %s оборвался где-то в темноте.",
        "cornerofeye.message.taken_back": "Свет вернул %s.",
        "cornerofeye.message.dream_start_taken": "Что-то держит тебя за ногу. Под тобой скребёт земля. Потом — ничего.",
        "cornerofeye.message.dream_won_taken": "Оно отпускает. Ты лежишь на земле там, куда оно тебя дотащило, и в руке что-то зажато.",
        "subtitles.cornerofeye.flesh": "Рвётся плоть",
    },
}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for lang, text in TEXT.items():
        data = dict(text)
        for i, line in enumerate(WHISPERS[lang]):
            data[f"cornerofeye.whisper.{i}"] = line
        for i, line in enumerate(SAID[lang]):
            data[f"cornerofeye.said.{i}"] = line
        for i, line in enumerate(SLEEP[lang]):
            data[f"cornerofeye.sleep.{i}"] = line
        for i, line in enumerate(WRITINGS[lang]):
            data[f"cornerofeye.writing.{i}"] = line
        for i, line in enumerate(MIMIC_SAID[lang]):
            data[f"cornerofeye.mimic_said.{i}"] = line
        for i, line in enumerate(SPLASHES[lang]):
            data[f"cornerofeye.splash.{i}"] = line
        for i, line in enumerate(GLITCHES[lang]):
            data[f"cornerofeye.glitch.{i}"] = line
        for kind, lines in TOOLTIPS[lang].items():
            for i, line in enumerate(lines):
                data[f"cornerofeye.tooltip.{kind}.{i}"] = line
        for chapter, pages in JOURNAL[lang].items():
            for i, page in enumerate(pages):
                data[f"cornerofeye.journal.{chapter}.{i}"] = page
        for d, pages in enumerate(CAMP_DIARIES[lang]):
            for i, page in enumerate(pages):
                data[f"cornerofeye.camp.diary.{d}.{i}"] = page
        for i, sign in enumerate(SIGNS[lang]):
            for j, line in enumerate(sign):
                data[f"cornerofeye.sign.{i}.{j}"] = line
        with open(os.path.join(OUT, lang + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    # The Java side counts lines per sign; make sure both languages agree.
    assert [len(s) for s in SIGNS["en_us"]] == [len(s) for s in SIGNS["ru_ru"]]
    assert len(WRITINGS["en_us"]) == len(WRITINGS["ru_ru"]) == 16
    # MenuHaunt.SPLASHES, GLITCHES and WHISPERS; the torch and lantern lists are fixed at 3.
    assert len(SPLASHES["en_us"]) == len(SPLASHES["ru_ru"]) == 10
    assert len(GLITCHES["en_us"]) == len(GLITCHES["ru_ru"]) == 8
    assert len(MIMIC_SAID["en_us"]) == len(MIMIC_SAID["ru_ru"]) == 6
    assert all(len(d) == 5 for lang in CAMP_DIARIES for d in CAMP_DIARIES[lang]) and len(CAMP_DIARIES["en_us"]) == len(CAMP_DIARIES["ru_ru"]) == 3
    for lang in TOOLTIPS:
        assert [len(v) for v in TOOLTIPS[lang].values()] == [8, 3, 3]
    print([len(s) for s in SIGNS["en_us"]])
