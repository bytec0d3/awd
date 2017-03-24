with open("normal_pois.wkt") as f:
    content = f.readlines()

content = [x.strip() for x in content]

for line in content:
    if len(line) > 0:
        line = line.replace("POINT (","").replace(")","")
        x=int(round(float(line.split(" ")[0])))
        y=int(round(float(line.split(" ")[1])))

        reminder=x%10
        x/=10
        x*=10

        if (reminder % 10) > 5:
            x+=10

        reminder=y%10
        y/=10
        y*=10

        if (reminder % 10) > 5:
            y+=10

        print("POINT ("+str(x)+" "+str(y)+")\n")