# do not repack jars
%define __jar_repack %{nil}
# produce .elX dist tag on both centos and redhat
%define dist %(/usr/lib/rpm/redhat/dist.sh)

Name:       xroad-base
Version:    %{xroad_version}
# release tag, e.g. 0.201508070816.el7 for snapshots and 1.el7 (for final releases)
Release:    %{rel}%{?snapshot}%{?dist}
Summary:    X-Road base components
Group:      Applications/Internet
License:    MIT
Requires(post): systemd
Requires(preun): systemd
Requires(postun): systemd
BuildRequires: systemd
Requires:  systemd
Requires:  rlwrap, crudini
Requires:  jre-1.8.0-headless >= 1.8.0.51
Requires:  sudo

%define src %{_topdir}/..

%description
X-Road base components and utilities

%prep
rm -rf base
cp -a %{srcdir}/common/base .
cd base
rm -rf etc/rcS.d
sed -i 's/JAVA_HOME=.*/JAVA_HOME=\/etc\/alternatives\/jre_1.8.0_openjdk/' etc/xroad/services/global.conf

%build

%install
cd base
cp -a * %{buildroot}

mkdir -p %{buildroot}%{_unitdir}
mkdir -p %{buildroot}%{_bindir}
mkdir -p %{buildroot}/usr/share/xroad/jlib
mkdir -p %{buildroot}/usr/share/xroad/lib
mkdir -p %{buildroot}/etc/xroad
mkdir -p %{buildroot}/etc/xroad/services
mkdir -p %{buildroot}/etc/xroad/conf.d/addons
mkdir -p %{buildroot}/usr/share/doc/%{name}
mkdir -p %{buildroot}/etc/xroad/ssl
mkdir -p %{buildroot}/var/lib/xroad/backup
mkdir -p %{buildroot}/etc/xroad/backup.d

ln -s /usr/share/xroad/jlib/common-db-1.0.jar %{buildroot}/usr/share/xroad/jlib/common-db.jar
ln -s /usr/share/xroad/jlib/postgresql-42.2.7.jar %{buildroot}/usr/share/xroad/jlib/postgresql.jar

cp -p %{srcdir}/../../../common-db/build/libs/common-db-1.0.jar %{buildroot}/usr/share/xroad/jlib/
cp -p %{srcdir}/../../../proxy-ui/build/libs/postgresql-42.2.7.jar %{buildroot}/usr/share/xroad/jlib/
cp -p %{srcdir}/default-configuration/common.ini %{buildroot}/etc/xroad/conf.d/
cp -p %{srcdir}/../../../LICENSE.txt %{buildroot}/usr/share/doc/%{name}/LICENSE.txt
cp -p %{srcdir}/../../../securityserver-LICENSE.info %{buildroot}/usr/share/doc/%{name}/securityserver-LICENSE.info
cp -p %{srcdir}/common/base/usr/share/xroad/db/liquibase-3.5.1.jar %{buildroot}/usr/share/xroad/db/liquibase-3.5.1.jar
cp -p %{srcdir}/common/base/usr/share/xroad/db/liquibase.sh %{buildroot}/usr/share/xroad/db/liquibase.sh
cp -p %{srcdir}/../../../../CHANGELOG.md %{buildroot}/usr/share/doc/%{name}/CHANGELOG.md

%clean
rm -rf %{buildroot}

%files
%defattr(0640,xroad,xroad,0751)
%attr(0440,root,root) %config /etc/sudoers.d/xroad-restore
%dir /etc/xroad
%dir /etc/xroad/ssl
%dir /etc/xroad/services
%dir /etc/xroad/conf.d
%dir /etc/xroad/conf.d/addons
%dir /var/lib/xroad
%dir /var/lib/xroad/backup
%config /etc/xroad/services/global.conf
%config /etc/xroad/conf.d/common.ini
%config /etc/xroad/ssl/openssl.cnf

%defattr(-,root,root,-)
%dir /usr/share/xroad
/usr/share/xroad/jlib/common-db.jar
/usr/share/xroad/jlib/common-db-1.0.jar
/usr/share/xroad/jlib/postgresql.jar
/usr/share/xroad/jlib/postgresql-*.jar
/usr/share/xroad/scripts/_backup_xroad.sh
/usr/share/xroad/scripts/generate_certificate.sh
/usr/share/xroad/scripts/_restore_xroad.sh
/usr/share/xroad/scripts/_backup_restore_common.sh
/usr/share/xroad/scripts/serverconf_migrations/add_acl.xsl
/usr/share/xroad/db/liquibase-3.5.1.jar
/usr/share/xroad/db/liquibase.sh
%doc /usr/share/doc/%{name}/LICENSE.txt
%doc /usr/share/doc/%{name}/securityserver-LICENSE.info
%doc /usr/share/doc/%{name}/CHANGELOG.md

%pre
if ! getent passwd xroad > /dev/null; then
useradd --system --home /var/lib/xroad --no-create-home --shell /bin/bash --user-group --comment "X-Road system user" xroad
fi

%verifyscript
# check validity of xroad user and group
if [ "`id -u xroad`" -eq 0 ]; then
echo "The xroad system user must not have uid 0 (root). Please fix this and reinstall this package." >&2
exit 1
fi
if [ "`id -g xroad`" -eq 0 ]; then
echo "The xroad system user must not have root as primary group. Please fix this and reinstall this package." >&2
exit 1
fi

%post
umask 027

# ensure home directory ownership
mkdir -p /var/lib/xroad/backup
su - xroad -c "test -O /var/lib/xroad && test -G /var/lib/xroad" || chown xroad:xroad /var/lib/xroad
chown xroad:xroad /var/lib/xroad/backup
chmod 0755 /var/lib/xroad
chmod -R go-w /var/lib/xroad

# nicer log directory permissions
mkdir -p -m1750 /var/log/xroad
chmod 1750 /var/log/xroad
chown xroad:adm /var/log/xroad
chmod -R go-w /var/log/xroad

#tmp folder
mkdir -p /var/tmp/xroad
chmod 1750 /var/tmp/xroad
chown xroad:xroad /var/tmp/xroad

# create socket directory
[ -d /var/run/xroad ] || install -d -m 2770 -o xroad -g xroad /var/run/xroad

#local overrides
test -f /etc/xroad/services/local.conf || touch /etc/xroad/services/local.conf
test -f /etc/xroad/conf.d/local.ini || touch /etc/xroad/conf.d/local.ini

chown -R xroad:xroad /etc/xroad/services/* /etc/xroad/conf.d/*
chmod -R o=rwX,g=rX,o= /etc/xroad/services/* /etc/xroad/conf.d/*

#enable xroad services by default
echo 'enable xroad-*.service' > %{_presetdir}/90-xroad.preset

%changelog
