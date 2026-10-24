"""Synthesizes the mod's sounds. Run from the repo root: python3 tools/gen_sounds.py  (needs numpy and ffmpeg)

Everything is mono so Minecraft can position it in 3D.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

SR = 44100
OUT = os.path.join(os.path.dirname(__file__), "..", "src", "main", "resources", "assets", "cornerofeye", "sounds")
rng = np.random.default_rng(666)


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def noise(seconds):
    return rng.standard_normal(int(SR * seconds))


def bandpass(x, low, high):
    spectrum = np.fft.rfft(x)
    freqs = np.fft.rfftfreq(len(x), 1 / SR)
    spectrum[(freqs < low) | (freqs > high)] = 0
    return np.fft.irfft(spectrum, len(x))


def lowpass(x, cutoff):
    return bandpass(x, 0, cutoff)


def env(n, attack, release, curve=2.0):
    e = np.ones(n)
    a, r = int(attack * SR), int(release * SR)
    if a:
        e[:a] = np.linspace(0, 1, a) ** curve
    if r:
        e[-r:] *= np.linspace(1, 0, r) ** curve
    return e


def osc_saw(freq):
    phase = np.cumsum(freq) / SR
    return 2 * (phase % 1.0) - 1


def osc_sine(freq):
    return np.sin(2 * np.pi * np.cumsum(freq) / SR)


def pad(x, seconds):
    return np.concatenate([x, np.zeros(int(SR * seconds))])


def write(name, x, gain_db=-1.0):
    x = np.asarray(x, dtype=np.float64)
    x -= np.mean(x)
    peak = np.max(np.abs(x)) or 1.0
    x = x / peak * (10 ** (gain_db / 20))
    pcm = (np.clip(x, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        path = tmp.name
    with wave.open(path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(SR)
        w.writeframes(pcm.tobytes())
    subprocess.run(["ffmpeg", "-loglevel", "error", "-y", "-i", path, "-ac", "1", "-c:a", "libvorbis", "-q:a", "5",
                    os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(path)


# ----------------------------------------------------------------------------- sounds

def breath(variant):
    """Slow, wet, rattling breath right next to you."""
    inhale_len, exhale_len = 1.1 + 0.2 * variant, 1.5 + 0.3 * variant
    inhale = bandpass(noise(inhale_len), 600, 3200) * env(int(SR * inhale_len), 0.8, 0.15, 1.5)
    exhale = bandpass(noise(exhale_len), 250, 1600) * env(int(SR * exhale_len), 0.08, 1.1, 1.5)
    # Vocal fry: the throat rattles on the way out.
    fry_rate = 38 + 6 * variant + 4 * np.sin(2 * np.pi * 0.7 * t(exhale_len))
    fry = 0.5 + 0.5 * np.sign(np.sin(2 * np.pi * np.cumsum(fry_rate) / SR))
    exhale = exhale * (0.45 + 0.55 * fry)
    growl = osc_saw(np.full(int(SR * exhale_len), 62.0 + 3 * variant) + rng.standard_normal(int(SR * exhale_len)) * 6)
    exhale += lowpass(growl, 400) * 0.25 * env(int(SR * exhale_len), 0.2, 1.0)
    return np.concatenate([inhale * 0.8, np.zeros(int(SR * 0.25)), exhale])


def skitter(variant):
    """Bony clicking, like too many joints moving at once."""
    length = 0.7
    out = np.zeros(int(SR * length))
    time = 0.0
    while time < length - 0.05:
        n = int(SR * 0.012)
        click = noise(0.012) * np.exp(-np.linspace(0, 9, n))
        click = bandpass(click, 1500 + rng.random() * 2500, 6500)
        start = int(time * SR)
        out[start:start + n] += click * (0.4 + rng.random() * 0.6)
        time += 0.018 + rng.random() * (0.07 if variant else 0.045)
    scrape = bandpass(noise(length), 2000, 5000) * env(len(out), 0.1, 0.4) * 0.08
    return out + scrape


def scream():
    """The jumpscare. Starts as a hit, turns into a shriek that is not a voice."""
    length = 1.7
    n = int(SR * length)
    tt = t(length)
    hit = lowpass(noise(length), 180) * np.exp(-tt * 9) * 2.5 + np.sin(2 * np.pi * 48 * tt) * np.exp(-tt * 6) * 1.5
    glide = np.interp(tt, [0, 0.12, 0.9, length], [0.55, 1.0, 1.08, 0.7])
    vibrato = 1 + 0.035 * np.sin(2 * np.pi * 9.5 * tt) + 0.01 * rng.standard_normal(n)
    voices = sum(osc_saw(f * glide * vibrato) * a for f, a in [(612, 1.0), (649, 0.8), (918, 0.6), (1297, 0.45), (1873, 0.3)])
    voices = bandpass(voices, 300, 7000)
    rasp = bandpass(noise(length), 1200, 6000) * (0.5 + 0.5 * np.sin(2 * np.pi * 31 * tt))
    shriek = (voices + rasp * 0.8) * env(n, 0.04, 0.5, 1.2)
    return np.tanh(2.8 * (hit + shriek * 0.9))


def wail():
    """Caught in the light: a long, falling, agonised wail."""
    length = 3.0
    n = int(SR * length)
    tt = t(length)
    base = np.interp(tt, [0, 0.3, 2.2, length], [380, 520, 300, 190])
    tremolo = 1 + 0.06 * np.sin(2 * np.pi * 6.5 * tt)
    voice = sum(osc_saw(base * m * tremolo) * a for m, a in [(1.0, 1.0), (1.012, 0.8), (0.497, 0.5), (1.5, 0.3)])
    # Rough vowel formants, "aaah" closing into "ooh".
    a_formant = bandpass(voice, 650, 1300)
    o_formant = bandpass(voice, 300, 800)
    mix = np.interp(tt, [0, length], [1.0, 0.2])
    voice = a_formant * mix + o_formant * (1 - mix) + bandpass(voice, 2400, 3200) * 0.3
    hiss = bandpass(noise(length), 3000, 9000) * 0.15
    return np.tanh(2.0 * (voice + hiss) * env(n, 0.05, 1.2, 1.3))


def vanish():
    """A sharp inward breath, played backwards: something stopping being there."""
    length = 0.9
    n = int(SR * length)
    swell = bandpass(noise(length), 200, 2500) * np.linspace(0, 1, n) ** 3
    swell[-int(SR * 0.02):] *= np.linspace(1, 0, int(SR * 0.02))
    sub = np.sin(2 * np.pi * 55 * t(length)) * np.linspace(0, 1, n) ** 4 * 0.6
    return swell + sub


def banish():
    """It comes apart: a rising shriek collapsing into a boom and a long, clean ring."""
    rise_len = 2.0
    tt = t(rise_len)
    rise = sum(osc_saw(np.interp(tt, [0, rise_len], [300, 1400]) * m) * a for m, a in [(1, 1), (1.06, 0.7), (1.5, 0.4)])
    rise = bandpass(rise, 200, 8000) * np.linspace(0.2, 1, len(tt)) ** 2
    rise = np.tanh(2.5 * (rise + bandpass(noise(rise_len), 2000, 9000) * np.linspace(0, 1, len(tt))))
    tail_len = 4.0
    tt2 = t(tail_len)
    boom = lowpass(noise(tail_len), 150) * np.exp(-tt2 * 2.5) * 3 + np.sin(2 * np.pi * 38 * tt2) * np.exp(-tt2 * 1.8) * 2
    ring = sum(np.sin(2 * np.pi * f * tt2) * np.exp(-tt2 * d) * a for f, d, a in [(523, 0.6, 0.3), (784, 0.7, 0.2), (1046, 0.9, 0.15)])
    air = bandpass(noise(tail_len), 400, 3000) * np.exp(-tt2 * 1.2) * 0.4
    return np.concatenate([rise * 0.8, np.tanh(boom) + ring + air])


def whisper(variant):
    """Breathy syllables that are almost words."""
    length = 2.6
    n = int(SR * length)
    out = np.zeros(n)
    time = 0.05
    while time < length - 0.3:
        syl = 0.08 + rng.random() * 0.18
        m = int(SR * syl)
        s = noise(syl)
        if rng.random() < 0.3:
            s = bandpass(s, 4000, 9000) * 0.7  # "sss", "shh"
        else:
            f1 = 400 + rng.random() * 500
            f2 = 1200 + rng.random() * 1400
            s = bandpass(s, f1, f1 + 300) + bandpass(s, f2, f2 + 500) * 0.8 + bandpass(s, 2800, 3600) * 0.3
        start = int(time * SR)
        out[start:start + m] += s * np.hanning(m) * (0.5 + rng.random() * 0.5)
        time += syl + (0.02 + rng.random() * 0.12) * (1 + variant)
    return out * env(n, 0.2, 0.4)


def heartbeat():
    """Lub-dub."""
    length = 0.7
    tt = t(length)

    def thump(at, pitch, amp):
        local = np.clip(tt - at, 0, None)
        gate = (tt >= at).astype(float)
        return np.sin(2 * np.pi * pitch * local * (1 - 0.3 * local)) * np.exp(-local * 22) * gate * amp

    beat = thump(0.0, 58, 1.0) + thump(0.21, 50, 0.7)
    return np.tanh(1.6 * (beat + lowpass(noise(length), 120) * np.exp(-tt * 15) * 0.4))


def drone():
    """Low, beating hum with something high and wrong underneath."""
    length = 7.0
    n = int(SR * length)
    tt = t(length)
    low = np.sin(2 * np.pi * 41 * tt) + np.sin(2 * np.pi * 43.3 * tt) + 0.5 * np.sin(2 * np.pi * 82.4 * tt)
    high = (np.sin(2 * np.pi * 1760 * tt) + np.sin(2 * np.pi * 1864 * tt) + 0.6 * np.sin(2 * np.pi * 2489 * tt)) \
        * np.interp(tt, [0, 2.5, 4.5, length], [0, 0.12, 0.08, 0])
    wind = bandpass(noise(length), 150, 900) * (0.25 + 0.15 * np.sin(2 * np.pi * 0.31 * tt))
    return (low * 0.5 + high + wind) * env(n, 1.8, 2.5, 1.5)


def lantern_charge():
    """Glass and amethyst, winding up."""
    length = 1.6
    tt = t(length)
    rise = np.interp(tt, [0, length], [1.0, 1.6])
    partials = [(880, 1.0), (1318, 0.6), (2217, 0.4), (3150, 0.25)]
    tone = sum(osc_sine(np.full(len(tt), f) * rise) * a for f, a in partials)
    shimmer = 1 + 0.35 * np.sin(2 * np.pi * np.interp(tt, [0, length], [6, 22]) * tt)
    return tone * shimmer * np.linspace(0.1, 1, len(tt)) ** 1.5 * env(len(tt), 0.05, 0.05)


def lantern_flash():
    """A burst of white light, if light made a sound."""
    length = 1.5
    tt = t(length)
    burst = bandpass(noise(length), 1500, 12000) * np.exp(-tt * 6)
    thump = np.sin(2 * np.pi * 70 * tt) * np.exp(-tt * 12) * 1.2
    chord = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * 2.2) * a for f, a in [(1046, 0.5), (1318, 0.4), (1568, 0.35), (2093, 0.25)])
    return np.tanh(1.5 * (burst + thump + chord))


def glimpse():
    """The sting when it shows up at the edge of your screen: a stab of clashing strings."""
    length = 1.6
    n = int(SR * length)
    tt = t(length)
    vibrato = 1 + 0.006 * np.sin(2 * np.pi * 6.0 * tt)
    strings = sum(osc_saw(np.full(n, f) * vibrato * (1 + 0.002 * i)) * a
                  for i, (f, a) in enumerate([(740, 1.0), (784, 0.9), (831, 0.8), (1480, 0.35), (1568, 0.3)]))
    strings = bandpass(strings, 500, 6000)
    bow = bandpass(noise(length), 2000, 7000) * 0.3
    sting = (strings + bow) * np.exp(-tt * 2.6) * env(n, 0.012, 0.3)
    thud = lowpass(noise(length), 120) * np.exp(-tt * 14) * 2.0
    return np.tanh(1.8 * (sting + thud))


def player_breath():
    """Your own breathing: fast, shallow, shaking."""
    parts = []
    for i in range(3):
        inhale_len = 0.32 + rng.random() * 0.08
        exhale_len = 0.42 + rng.random() * 0.1
        shake = 1 + 0.35 * np.sin(2 * np.pi * 11 * t(inhale_len))
        inhale = bandpass(noise(inhale_len), 900, 4200) * np.hanning(int(SR * inhale_len)) * shake
        exhale = bandpass(noise(exhale_len), 400, 2400) * np.hanning(int(SR * exhale_len)) * 0.8
        parts += [inhale, np.zeros(int(SR * 0.05)), exhale, np.zeros(int(SR * (0.08 + rng.random() * 0.1)))]
    return np.concatenate(parts)


def tinnitus():
    """The ringing in your ears when it is far too close."""
    length = 4.5
    tt = t(length)
    tone = np.sin(2 * np.pi * 6200 * tt) + 0.6 * np.sin(2 * np.pi * 6310 * tt) + 0.25 * np.sin(2 * np.pi * 3100 * tt)
    return tone * env(len(tt), 0.4, 3.2, 1.6)


def dread_loop():
    """A seamless 8 second hum that sits under everything while it is near.

    Every tone completes a whole number of cycles and the noise is built in the frequency domain,
    so the end runs straight into the start with no click.
    """
    length = 8.0
    n = int(SR * length)
    tt = t(length)

    def looping_noise(low, high):
        freqs = np.fft.rfftfreq(n, 1 / SR)
        spectrum = np.zeros(len(freqs), dtype=complex)
        band = (freqs >= low) & (freqs <= high)
        spectrum[band] = np.exp(1j * rng.uniform(0, 2 * np.pi, band.sum()))
        x = np.fft.irfft(spectrum, n)
        return x / np.max(np.abs(x))

    low = np.sin(2 * np.pi * 55.0 * tt) + np.sin(2 * np.pi * 55.25 * tt) + 0.4 * np.sin(2 * np.pi * 82.5 * tt)
    throb = 0.75 + 0.25 * np.sin(2 * np.pi * 0.5 * tt)
    air = looping_noise(120, 700) * 0.35
    whine = (np.sin(2 * np.pi * 1320.0 * tt) + np.sin(2 * np.pi * 1398.125 * tt)) * 0.04 * (0.5 + 0.5 * np.sin(2 * np.pi * 0.25 * tt))
    return (low * 0.45 * throb + air + whine)


def timer_tick():
    """A dry clock tick with a low knock under it."""
    length = 0.3
    tt = t(length)
    click = bandpass(noise(length), 2500, 7000) * np.exp(-tt * 260)
    knock = np.sin(2 * np.pi * 880 * tt) * np.exp(-tt * 60) * 0.6 + np.sin(2 * np.pi * 70 * tt) * np.exp(-tt * 30) * 0.5
    return np.tanh(2.0 * (click + knock))


def night_bell():
    """A deep bell somewhere far off, slightly out of tune with itself."""
    length = 6.0
    tt = t(length)
    partials = [(73.0, 1.0, 0.6), (146.8, 0.6, 0.8), (174.5, 0.45, 1.0), (219.0, 0.35, 1.3), (292.6, 0.25, 1.7), (367.0, 0.15, 2.2)]
    bell = sum(np.sin(2 * np.pi * f * tt) * a * np.exp(-tt * d) for f, a, d in partials)
    strike = bandpass(noise(length), 300, 3000) * np.exp(-tt * 40) * 0.4
    return np.tanh(1.2 * (bell + strike)) * env(len(tt), 0.005, 1.5)


def crack():
    """Glass giving way slowly: a creak under it, then sharp ticks running along the crack, faster and faster."""
    length = 1.4
    tt = t(length)
    creak = bandpass(osc_saw(170 + 50 * np.sin(2 * np.pi * 3 * tt)), 150, 1200) * 0.12 * env(len(tt), 0.2, 0.8)
    ticks = np.zeros(len(tt))
    when, gap = 0.22, 0.13
    while when < 1.15:
        start = int(when * SR)
        burst = bandpass(noise(0.04), 2000, 9000) * np.exp(-t(0.04) * 180) * (0.5 + rng.random() * 0.5)
        end = min(len(ticks), start + len(burst))
        ticks[start:end] += burst[:end - start]
        when += gap * (0.5 + rng.random())
        gap = max(gap * 0.8, 0.015)
    return np.tanh(2.0 * (ticks + creak)) * env(len(tt), 0.01, 0.3)


def warding_bell():
    """A small handbell, struck twice by its clapper: bright, slightly sour partials ringing out."""
    length = 1.6
    tt = t(length)
    partials = [(1318.0, 1.0, 3.2), (2894.0, 0.55, 5.0), (3610.0, 0.35, 6.5), (5120.0, 0.2, 9.0)]
    out = np.zeros(len(tt))
    for start, gain in [(0.0, 1.0), (0.11, 0.45)]:
        n = int(start * SR)
        sub = tt[:len(tt) - n]
        strike = sum(np.sin(2 * np.pi * f * sub) * a * np.exp(-sub * d) for f, a, d in partials)
        strike = strike * (1 + 0.15 * np.sin(2 * np.pi * 6 * sub))
        out[n:] += strike * gain
    return np.tanh(1.3 * out) * env(len(tt), 0.002, 0.4)


if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    write("the_one_breath1", breath(0), -3)
    write("the_one_breath2", breath(1), -3)
    write("the_one_skitter1", skitter(0), -4)
    write("the_one_skitter2", skitter(1), -4)
    write("the_one_scream", scream(), -0.5)
    write("the_one_wail", wail(), -1)
    write("the_one_vanish", vanish(), -3)
    write("the_one_banish", banish(), -1)
    write("whisper1", whisper(0), -4)
    write("whisper2", whisper(1), -4)
    write("heartbeat", heartbeat(), -1)
    write("drone", drone(), -2)
    write("lantern_charge", lantern_charge(), -4)
    write("lantern_flash", lantern_flash(), -1)
    write("glimpse", glimpse(), -1)
    write("player_breath", player_breath(), -2)
    write("tinnitus", tinnitus(), -8)
    write("dread_loop", dread_loop(), -3)
    write("timer_tick", timer_tick(), -2)
    write("night_bell", night_bell(), -2)
    write("crack", crack(), -3)
    write("warding_bell", warding_bell(), -3)
    print("sounds written to", os.path.normpath(OUT))
