filename="numbers"
i=0
with open(filename, 'w') as output:
    while (True):
        output.write(str(i)+"\n")
        i=i+1
        if (i%1000000==0):
            print i