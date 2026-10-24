"""Sends commands to the dev server over RCON: python3 tools/dev/rcon.py "time set noon" "sleep 2" "tp Dev 0 -60 0"

Needs enable-rcon=true and rcon.password=test in run/server.properties (tools/dev/setup_run.sh writes them).
"""
import socket, struct, sys, time

def packet(rid, kind, body):
    data = struct.pack('<ii', rid, kind) + body.encode('utf-8') + b'\x00\x00'
    return struct.pack('<i', len(data)) + data

def recv(s):
    raw = b''
    while len(raw) < 4:
        raw += s.recv(4 - len(raw))
    n = struct.unpack('<i', raw)[0]
    data = b''
    while len(data) < n:
        data += s.recv(n - len(data))
    rid, kind = struct.unpack('<ii', data[:8])
    return data[8:-2].decode('utf-8', 'replace')

s = socket.create_connection(('127.0.0.1', 25575))
s.sendall(packet(1, 3, 'test')); recv(s)
for cmd in sys.argv[1:]:
    if cmd.startswith('sleep '):
        time.sleep(float(cmd.split()[1])); continue
    s.sendall(packet(2, 2, cmd))
    print('>', cmd, '\n ', recv(s))
