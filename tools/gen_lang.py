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
        ],
        "three": [
            "It screams when you catch it.\n\nThree times. Catch it three times and it leaves this world for good.",
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
        ],
        "three": [
            "Когда его ловишь, оно кричит.\n\nТри раза. Поймай его трижды, и оно уйдёт из этого мира навсегда.",
        ],
        "hunt": [
            "Седьмая ночь. Оно больше не ждёт. Теперь оно проходит сквозь стены.\n\nЕсли у тебя ещё нет фонаря, ищи обрывки, пока можешь.",
            "Я поймал его один раз.\n\nОно сейчас в углу. Я его вижу.\n\nНе смотри на него.",
        ],
    },
}

TEXT = {
    "en_us": {
        "entity.cornerofeye.the_one": "The One in the Corner",
        "item.cornerofeye.the_one_spawn_egg": "The One in the Corner Spawn Egg",
        "item.cornerofeye.shadow_scrap": "Shadow Scrap",
        "item.cornerofeye.shadow_scrap.tooltip": "Cold to the touch. It was standing right here.",
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
    },
    "ru_ru": {
        "entity.cornerofeye.the_one": "Тот, что в углу",
        "item.cornerofeye.the_one_spawn_egg": "Яйцо призыва Того, что в углу",
        "item.cornerofeye.shadow_scrap": "Обрывок тени",
        "item.cornerofeye.shadow_scrap.tooltip": "Холодный на ощупь. Оно стояло прямо здесь.",
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
        for chapter, pages in JOURNAL[lang].items():
            for i, page in enumerate(pages):
                data[f"cornerofeye.journal.{chapter}.{i}"] = page
        for i, sign in enumerate(SIGNS[lang]):
            for j, line in enumerate(sign):
                data[f"cornerofeye.sign.{i}.{j}"] = line
        with open(os.path.join(OUT, lang + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    # The Java side counts lines per sign; make sure both languages agree.
    assert [len(s) for s in SIGNS["en_us"]] == [len(s) for s in SIGNS["ru_ru"]]
    print([len(s) for s in SIGNS["en_us"]])
