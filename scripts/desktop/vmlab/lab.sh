# Shared settings for the Argosy Desktop VM test lab. Sourced by the other vmlab scripts.
# VMs are named argosy-lab-<profile>; nothing here touches any other domain.
LAB=${ARGOSY_LAB_DIR:-$(cat "$(dirname "${BASH_SOURCE[0]}")/lab-dir.local" 2>/dev/null)}
[ -n "$LAB" ] || { echo "set ARGOSY_LAB_DIR or write the lab folder to scripts/desktop/vmlab/lab-dir.local" >&2; exit 2; }
# Session libvirt: the VMs run as the owner (the drive is private to them) and stay apart from the
# system VMs. Guests use user-mode networking and reach the host, and the RomM testbed, at 10.0.2.2.
export LIBVIRT_DEFAULT_URI=qemu:///session
ROMM_FROM_GUEST=http://10.0.2.2:8093
PROFILES="fedora-gnome fedora-kde ubuntu-gnome"

lab_domain() {
    case "$1" in
        fedora-gnome|fedora-kde|ubuntu-gnome) echo "argosy-lab-$1" ;;
        *) echo "unknown profile '$1' (one of: $PROFILES)" >&2; exit 2 ;;
    esac
}

lab_base_image() {
    case "$1" in
        fedora-*) echo "$LAB/images/fedora-44-cloud.qcow2" ;;
        ubuntu-*) echo "$LAB/images/ubuntu-26.04-cloud.img" ;;
    esac
}
