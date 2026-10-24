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

JOURNAL = {
    "en_us": [
        "If you are reading this, it found you too.\n\nI don't know how many nights I have left. I'll write down what I've learned.",
        "It stands in the dark, right at the edge of your vision.\n\nTurn to look and there is nothing there. You can only ever see it from the corner of your eye.",
        "It puts out the lights. Torches, lanterns, campfires. Only the ones you aren't looking at.\n\nOne by one, until it's dark enough.",
        "It won't step into light, and it can't touch you while you stand in it.\n\nGlowstone doesn't burn. Neither do sea lanterns. Those it can't put out.",
        "Walls mean nothing to it.\n\nAt first it only watches. Then it comes a little closer every night.\n\nBy the seventh night it stops waiting.",
        "Where it stood, it leaves scraps. Cold, like a torn piece of shadow.\n\nWalk to where you saw it. It won't let you get close, but the scrap will be there.",
        "Collect them while it is still only watching.\n\nOnce the hunt begins it leaves almost nothing behind.",
        "A lantern, four amethyst shards and four scraps make a Lantern of Revelation.\n\nHold it to charge. Let go to flash.",
        "If it is anywhere in sight when the light goes off, even at the very edge, it is caught.\n\nEvery flash burns one scrap. Don't waste them.",
        "Three times. Catch it three times and it leaves for good.\n\nI caught it once.\n\nIt's in the corner now. I can see it.\n\nDon't look at it.",
    ],
    "ru_ru": [
        "Если ты это читаешь, оно нашло и тебя.\n\nНе знаю, сколько ночей у меня осталось. Запишу всё, что успел понять.",
        "Оно стоит в темноте, на самом краю зрения.\n\nПовернёшься посмотреть — там никого. Его можно увидеть только краем глаза.",
        "Оно гасит свет. Факелы, фонари, костры. Только те, на которые ты не смотришь.\n\nОдин за другим, пока не станет достаточно темно.",
        "В свет оно не заходит и не может тронуть тебя, пока ты стоишь в нём.\n\nСветокамень не горит. Морской фонарь тоже. Их оно погасить не может.",
        "Стены для него ничего не значат.\n\nСначала оно просто смотрит. Потом каждую ночь подходит чуть ближе.\n\nК седьмой ночи оно перестаёт ждать.",
        "Там, где оно стояло, остаются обрывки. Холодные, как клочок тени.\n\nИди туда, где его видел. Близко оно не подпустит, но обрывок останется.",
        "Собирай их, пока оно только смотрит.\n\nКогда начнётся охота, оно почти ничего не оставляет.",
        "Фонарь, четыре осколка аметиста и четыре обрывка — это Фонарь откровения.\n\nДержи, чтобы зарядить. Отпусти — будет вспышка.",
        "Если в момент вспышки оно хоть где-то в поле зрения, даже на самом краю, — оно попалось.\n\nКаждая вспышка сжигает один обрывок. Не трать зря.",
        "Три раза. Поймай его трижды, и оно уйдёт навсегда.\n\nЯ поймал один раз.\n\nОно сейчас в углу. Я его вижу.\n\nНе смотри на него.",
    ],
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
        "cornerofeye.journal.title": "Someone's Notes",
        "cornerofeye.message.journal": "There's a notebook in your pocket. You don't remember picking it up.",
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
        "cornerofeye.journal.title": "Чьи-то записи",
        "cornerofeye.message.journal": "В кармане чужая записная книжка. Ты не помнишь, откуда она.",
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
        for i, page in enumerate(JOURNAL[lang]):
            data[f"cornerofeye.journal.page.{i}"] = page
        for i, sign in enumerate(SIGNS[lang]):
            for j, line in enumerate(sign):
                data[f"cornerofeye.sign.{i}.{j}"] = line
        with open(os.path.join(OUT, lang + ".json"), "w", encoding="utf-8") as f:
            json.dump(data, f, indent=2, ensure_ascii=False)
            f.write("\n")
    # The Java side counts lines per sign; make sure both languages agree.
    assert [len(s) for s in SIGNS["en_us"]] == [len(s) for s in SIGNS["ru_ru"]]
    print([len(s) for s in SIGNS["en_us"]])
