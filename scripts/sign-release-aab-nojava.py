#!/usr/bin/env python3
"""Locally sign the pinned DZlog v1.0.0 Android App Bundle without Java.

Requires Termux Python 3 + OpenSSL. Does not upload keys or passwords.
A PKCS12 private key is extracted only to a temporary folder in Termux's
private app storage and removed even if signing fails.
"""
import base64
import getpass
import hashlib
import os
import shutil
import subprocess
import sys
import tempfile
import zipfile
from pathlib import Path

EXPECTED_ARTIFACT_SHA256 = 'd2aac0ad36b0047464223ad5d0546676c0614f4e0be9019f782db2aecc07e6d8'
EXPECTED_UPLOAD_CERT_SHA256 = '02:E6:59:D3:37:C4:E2:63:97:E4:0A:43:54:C7:AD:1B:C5:2A:EF:88:EF:33:26:33:00:89:7D:A3:93:9B:D8:00'
OUTPUT_NAME = 'DZlog-v1.0.0-vc3-signed.aab'


def fail(message):
    raise RuntimeError(message)


def b64sha256(data):
    return base64.b64encode(hashlib.sha256(data).digest()).decode('ascii')


def manifest_attribute(name, value):
    # JAR manifest lines wrap to <=72 UTF-8 bytes, using CRLF and a leading
    # space on every continuation line. Use 70 bytes to leave margin.
    rest = f'{name}: {value}'.encode('utf-8')
    out = bytearray()
    while len(rest) > 70:
        out.extend(rest[:70] + b'\r\n')
        rest = b' ' + rest[70:]
    out.extend(rest + b'\r\n')
    return bytes(out)


def manifest_file_section(name, value):
    return (manifest_attribute('Name', name)
            + manifest_attribute('SHA-256-Digest', value)
            + b'\r\n')


def run(*args, env=None):
    subprocess.run(args, check=True, env=env,
                   stdout=subprocess.DEVNULL, stderr=subprocess.PIPE)


def file_sha256(path):
    h = hashlib.sha256()
    with path.open('rb') as stream:
        for chunk in iter(lambda: stream.read(1024 * 1024), b''):
            h.update(chunk)
    return h.hexdigest()


def sign(artifact, keystore):
    if shutil.which('openssl') is None:
        fail('OpenSSL CLI is missing. In Termux run: pkg install -y openssl-tool, then run again. No keystore password was read.')
    output = artifact.parent / OUTPUT_NAME
    if output.exists():
        fail(f'Signed output already exists: {output}. Do not overwrite it.')
    if not artifact.is_file() or not keystore.is_file():
        fail('Unsigned ZIP or upload JKS not found. Check the provided paths.')

    print('[DZlog] 1/6 Verify the exact release artifact SHA-256.', flush=True)
    checksum = file_sha256(artifact)
    if checksum != EXPECTED_ARTIFACT_SHA256:
        fail('Release artifact SHA-256 mismatch. Signing stopped before opening the key. '
             f'Actual SHA-256: {checksum}')

    # All temporary files (including unencrypted private key) are strictly
    # confined to Termux's private home, never the shared Download folder.
    with tempfile.TemporaryDirectory(prefix='dzlog-sign-', dir=Path.home()) as temp:
        wd = Path(temp)
        os.chmod(wd, 0o700)
        unsigned_aab = wd / 'unsigned.aab'
        print('[DZlog] 2/6 Extract unsigned Android App Bundle.', flush=True)
        with zipfile.ZipFile(artifact, 'r') as archive:
            entries = [entry for entry in archive.infolist()
                       if entry.filename == 'app-release.aab']
            if len(entries) != 1:
                fail(f'Expected one app-release.aab; found {len(entries)}.')
            with archive.open(entries[0]) as src, unsigned_aab.open('wb') as dst:
                shutil.copyfileobj(src, dst)
        with zipfile.ZipFile(unsigned_aab) as zipped:
            if 'base/manifest/AndroidManifest.xml' not in zipped.namelist():
                fail('Android manifest is missing; invalid unsigned AAB.')

        if not sys.stdin.isatty():
            fail('Run directly in interactive Termux. Password entry must be hidden.')
        password = getpass.getpass('Enter upload keystore password (hidden): ')
        if not password:
            fail('Empty upload keystore password.')

        print('[DZlog] 3/6 Verify local PKCS12 upload key fingerprint.', flush=True)
        env = os.environ.copy()
        env['DZLOG_P12_PASSWORD'] = password
        del password
        private_pem = wd / 'key.pem'
        cert_pem = wd / 'cert.pem'
        try:
            run('openssl', 'pkcs12', '-in', str(keystore),
                '-nocerts', '-nodes', '-out', str(private_pem),
                '-passin', 'env:DZLOG_P12_PASSWORD', env=env)
            os.chmod(private_pem, 0o600)
            run('openssl', 'pkcs12', '-in', str(keystore),
                '-clcerts', '-nokeys', '-out', str(cert_pem),
                '-passin', 'env:DZLOG_P12_PASSWORD', env=env)
        except subprocess.CalledProcessError:
            fail('Could not unlock local PKCS12 key. Check password/key format. '
                 'Nothing was signed.')
        finally:
            del env
        cert_der = wd / 'cert.der'
        run('openssl', 'x509', '-in', str(cert_pem), '-outform', 'DER',
            '-out', str(cert_der))
        raw = hashlib.sha256(cert_der.read_bytes()).hexdigest().upper()
        fingerprint = ':'.join(raw[n:n+2] for n in range(0, len(raw), 2))
        if fingerprint != EXPECTED_UPLOAD_CERT_SHA256:
            fail('Local signing certificate SHA-256 differs from the approved '
                 'DZlog Play upload certificate. Nothing was signed.')

        print('[DZlog] 4/6 Generate JAR manifest and signature file.', flush=True)
        manifest_main = (manifest_attribute('Manifest-Version', '1.0')
                         + manifest_attribute('Created-By', 'DZlog Android local signer')
                         + b'\r\n')
        manifest = bytearray(manifest_main)
        signature_sections = []
        with zipfile.ZipFile(unsigned_aab, 'r') as original:
            files = original.infolist()
            seen = set()
            for item in files:
                name = item.filename
                if name in seen:
                    fail('Duplicate entries found in unsigned AAB.')
                seen.add(name)
                if item.is_dir():
                    continue
                if name.upper().startswith('META-INF/'):
                    leaf = name.upper().rsplit('/', 1)[-1]
                    if leaf == 'MANIFEST.MF' or leaf.endswith(
                            ('.SF', '.RSA', '.DSA', '.EC')) or leaf.startswith('SIG-'):
                        fail('Unsigned AAB already contains a JAR signature.')
                with original.open(item) as member:
                    h = hashlib.sha256()
                    for chunk in iter(lambda: member.read(1024 * 1024), b''):
                        h.update(chunk)
                entry = manifest_file_section(
                    name, base64.b64encode(h.digest()).decode('ascii'))
                manifest.extend(entry)
                signature_sections.append((name, b64sha256(entry)))

            signature_main = (
                manifest_attribute('Signature-Version', '1.0')
                + manifest_attribute('Created-By', 'DZlog Android local signer')
                + manifest_attribute('SHA-256-Digest-Manifest', b64sha256(manifest))
                + manifest_attribute('SHA-256-Digest-Manifest-Main-Attributes',
                                     b64sha256(manifest_main))
                + b'\r\n')
            sf = bytearray(signature_main)
            for name, section_digest in signature_sections:
                sf.extend(manifest_file_section(name, section_digest))
            sf_path = wd / 'DZLOG.SF'
            sf_path.write_bytes(sf)
            rsa_path = wd / 'DZLOG.RSA'

            print('[DZlog] 5/6 Sign using OpenSSL CMS, without Java.', flush=True)
            run('openssl', 'cms', '-sign', '-binary', '-in', str(sf_path),
                '-signer', str(cert_pem), '-inkey', str(private_pem),
                '-outform', 'DER', '-md', 'sha256', '-nosmimecap',
                '-out', str(rsa_path))
            run('openssl', 'cms', '-verify', '-binary', '-inform', 'DER',
                '-in', str(rsa_path), '-content', str(sf_path),
                '-noverify', '-out', os.devnull)

            signed = wd / 'signed.aab'
            with zipfile.ZipFile(signed, 'w') as result:
                result.writestr('META-INF/MANIFEST.MF', manifest)
                result.writestr('META-INF/DZLOG.SF', sf)
                result.writestr('META-INF/DZLOG.RSA', rsa_path.read_bytes())
                for item in files:
                    if item.is_dir():
                        continue
                    with original.open(item) as src, result.open(item, 'w') as dst:
                        shutil.copyfileobj(src, dst, 1024 * 1024)

        print('[DZlog] 6/6 Verify signed bundle integrity.', flush=True)
        with zipfile.ZipFile(signed, 'r') as verify:
            corrupted = verify.testzip()
            if corrupted:
                fail(f'Signed ZIP integrity error: {corrupted}')
            if len(verify.infolist()) != len(files) + 3:
                fail('Signed AAB contains an unexpected number of ZIP entries.')
            for name in ('META-INF/MANIFEST.MF',
                         'META-INF/DZLOG.SF', 'META-INF/DZLOG.RSA'):
                if name not in verify.namelist():
                    fail(f'JAR signature item missing: {name}')

        shutil.copyfile(signed, output)
        print('[DZlog] SUCCESS: signed DZlog AAB is ready for Play INTERNAL testing.')
        print(f'[DZlog] File: {output}')
        print(f'[DZlog] SHA-256: {file_sha256(output)}')
        print('[DZlog] Do not upload your JKS, password, or unsigned ZIP.')


def main():
    if len(sys.argv) != 3:
        print('Usage: python sign-release-aab-nojava.py <artifact.zip> <upload.jks>')
        sys.exit(2)
    artifact = Path(sys.argv[1]).expanduser().resolve()
    keystore = Path(sys.argv[2]).expanduser().resolve()
    try:
        sign(artifact, keystore)
    except (OSError, RuntimeError, zipfile.BadZipFile,
            subprocess.CalledProcessError) as exc:
        print(f'[ERROR] {exc}', file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()
