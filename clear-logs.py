import ftplib, time
ftp = ftplib.FTP("10.15.40.2")
print("Login response:", ftp.login())
fnames = [x for x in ftp.nlst() if x.startswith("log")]
print("Files:", fnames)
if input('Listed. Retrieve? (y/n)') == 'y':
	for filename in fnames:
		local_filename = "ftp%f-%s" % (time.time(), filename)
		with open(local_filename, 'wb') as f:
			print("Fetching", local_filename)
			print("=>", ftp.retrbinary('RETR '+ filename, f.write))
	print("Done!")
if input('Retrieved. Delete? (y/n) ') == 'y':
	print("Deleting...")
	for filename in fnames:
		print("Deleting", filename)
		print("=>", ftp.delete(filename))
	print("Done!")
ftp.close()
print("Bye.")
