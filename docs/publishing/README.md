# Публикация: картинки, промпты, тексты

Всё для страницы мода на CurseForge / Modrinth и для обложек.

## Рендеры монстра

Папка `renders/`. Это кадры настоящей модели из игры, 1920×1080, вырезанные с зелёного фона, PNG с прозрачностью. Их можно ставить прямо на баннеры или давать нейросети как образец внешности.

| Файл | Что на нём |
|---|---|
| `stage1_standing.png` | Первая стадия: высокий, тощий, голова набок, рот полон зубов |
| `stage2_standing.png` | Вторая стадия: чёрные вены, засохшая кровь у рта |
| `stage3_standing.png` | Третья стадия: шесть красных глаз, треснувшая кожа, когти длиннее |
| `stage2_face.png`, `stage3_face.png` | Лицо крупным планом |
| `stage3_reaching.png` | Тянется к зрителю: когти почти в объектив |
| `stage3_arms_up.png` | Руки подняты вверх (поднимается из глубины) |
| `stage3_revealed.png` | Пойман вспышкой: руки раскинуты, рот открыт |
| `stage3_crawling.png` | Ползёт на четвереньках, как паук, смотрит на тебя |
| `stage3_chase.png` | Погоня: ползёт прямо на тебя |

`icon.png` (512×512) — иконка мода, та же лежит в jar (`cornerofeye.png`, её видно в списке модов).

Скриншоты из игры для страницы мода лежат в `docs/screenshots/`.

Как снять новые рендеры, описано в `docs/DEVELOPMENT.md` и `tools/chroma_key.py`.

## Как его описывать нейросети

Общее описание монстра, его стоит вставлять во все промпты (на английском, нейросети лучше понимают английский):

> an extremely tall, gaunt humanoid creature made of blocky Minecraft-style voxels, twice the height of a person, impossibly thin limbs, arms hanging down to its knees ending in three long black claws, pale grey-beige dead skin with dark veins and dried blood, a small square head tilted sharply to one side, a lipless mouth stretched too wide full of crooked yellowed teeth, six small glowing red eyes, black hair stuck to the scalp

Если генератор умеет брать картинку-образец (Midjourney `--cref` / `--sref`, image prompt в Leonardo, IP-Adapter в Stable Diffusion, «референс» в Kandinsky / Шедевруме), дайте ему `renders/stage3_standing.png` и `renders/stage3_face.png`. Так внешность получится ближе к игре.

## Промпты

### 1. Логотип (надпись)

```
horror game logo, the words "CORNER OF THE EYE" in tall narrow cracked white letters, letters slightly uneven as if written by something that has only seen writing, a thin vertical crack runs through the "O" like a pupil, one tall gaunt blocky silhouette with a tilted head standing in the far right corner of the frame half out of view, pure black background, faint red glow from below, minimal, high contrast, centered composition, Minecraft horror mod logo --ar 3:1
```

Русский вариант надписи: замените `"CORNER OF THE EYE"` на `"КРАЕМ ГЛАЗА"` (кириллицу лучше понимают GPT-Image, Ideogram, Kandinsky; Midjourney часто путает буквы).

### 2. Баннер страницы мода (широкий)

```
cinematic wide shot inside a dark Minecraft-style wooden house at night, a single torch on the wall is going out with a thin trail of smoke, everything beyond its small circle of light is pitch black, in the far left corner of the room, barely visible at the very edge of the frame, stands [MONSTER DESCRIPTION] watching, its red eyes are the only bright points in the darkness, blocky voxel world, volumetric darkness, dread, film grain, horror atmosphere, empty space on the right for a title --ar 3:1
```

`[MONSTER DESCRIPTION]` — вставьте общее описание из раздела выше.

### 3. Иконка / превью (квадрат)

```
extreme close-up portrait of [MONSTER DESCRIPTION], blocky Minecraft voxel style, the head tilted almost sideways, filling the frame, six red eyes glowing, teeth in the dark, deep red rim light from below, pitch black background, horror, high detail, centered --ar 1:1
```

### 4. Постер (вертикальный)

```
vertical horror movie poster, a lone Minecraft player holding a torch stands in a dark field at night under a huge swollen blood-red moon, behind the player, much taller than them and only visible at the very edge of the light, [MONSTER DESCRIPTION] crawls on all fours like a spider, its head turned towards the viewer, all the torches in the distance are going out one by one, blocky voxel world, red and black palette, fog, tagline at the bottom "ONLY FROM THE CORNER OF YOUR EYE" --ar 2:3
```

Русская строка внизу: `"ТОЛЬКО КРАЕМ ГЛАЗА"`.

### 5. Обложка для видео (YouTube)

```
YouTube thumbnail, a terrified Minecraft player character in the foreground on the right looking over their shoulder, behind them in a dark doorway [MONSTER DESCRIPTION] squeezes in on all fours, red eyes, a countdown timer "0:05" in big red pixel digits in the top corner, high contrast, saturated reds, dramatic lighting, blocky voxel style --ar 16:9
```

### 6. Кровавая луна (фон / сплэш)

```
a vast flat Minecraft-style landscape at night under an enormous blood-red moon with dark scars across it, the sky and fog are deep red, a small village in the distance with all lights out, at the edge of the frame a very tall thin figure with a tilted head stands perfectly still, voxel world, eerie, cinematic, wide --ar 16:9
```

### Негативный промпт (для Stable Diffusion и похожих)

```
cute, cartoonish, smiling, friendly, bright daylight, colorful, realistic human, smooth organic shapes, extra heads, text errors, watermark, signature, blurry, low quality, jpeg artifacts
```

### Советы

- Для всего «по-майнкрафтовски» добавляйте `blocky voxel style, Minecraft` и не просите фотореализм: иначе получится обычный человекоподобный монстр.
- Самое узнаваемое в нём: голова набок, слишком длинные руки до колен с тремя когтями, слишком широкий рот. Если чего-то не хватает, повторите это в промпте отдельно.
- Главная идея мода — его видно только краем глаза. Хорошо работают кадры, где он у самого края картинки, а в центре пусто.

## Тексты для страницы мода

### Коротко (одна строка)

- RU: Оно живёт там, куда не достаёт взгляд. Его видно только краем глаза.
- EN: Something lives where your eyes don't reach. You can only see it from the corner of your eye.

### Описание (EN)

> **Corner of the Eye** is a slow-burning horror mod for Minecraft 1.20.1 (Forge), for singleplayer and co-op.
>
> Something lives in your world. Look straight at it and nothing is there; it only exists at the edge of your vision. It puts out the lights you aren't looking at, walks through walls, and every night it comes a little closer. There is no fear meter: you'll know it is near by your heartbeat, your breathing and the world going quiet.
>
> - It watches first, and only then hunts. Its touch wounds, and later it kills.
> - Scripted moments: it crawls in through your door, peeks from behind a tree, hangs from a cave ceiling, rises from under the water, empties a village while you aren't looking. A timer tells you what to do to survive.
> - Every light can go out: torches burn out, furnaces stop, glowstone dies, and nothing comes back on by itself.
> - Night cards, blood moons, marks and inhuman writing in blood, animals that sense it, a mimic wearing a friend's face, nightmares where you must find your bed in the dark.
> - It can be beaten: gather the scraps it leaves behind, build the Lantern of Revelation, and catch it three times. The last time only counts under a blood moon.

### Описание (RU)

> **Краем глаза** — медленный хоррор-мод для Minecraft 1.20.1 (Forge), для одиночной игры и кооператива.
>
> В твоём мире кто-то живёт. Посмотришь прямо — никого нет: оно существует только на краю зрения. Оно гасит свет, на который ты не смотришь, проходит сквозь стены и каждую ночь подбирается ближе. Шкалы страха нет: о том, что оно рядом, скажут сердцебиение, дыхание и затихший мир.
>
> - Сначала оно наблюдает и только потом охотится. Его касание ранит, а позже убивает.
> - Страшные моменты: вползает в дверь, выглядывает из-за дерева, свешивается с потолка пещеры, поднимается из-под воды, опустошает деревню, пока ты не смотришь. Таймер подскажет, что сделать, чтобы выжить.
> - Гаснет любой свет: факелы сгорают, печи встают, светокамень умирает, и сам ничего не загорается.
> - Ночные карточки, кровавая луна, следы и нечеловеческие надписи кровью, животные, которые его чуют, подражатель с лицом друга, кошмары, где нужно найти свою кровать в темноте.
> - Его можно одолеть: собери обрывки тени, сделай Фонарь откровения и поймай его трижды. Последний раз засчитывается только под кровавой луной.
