#!/bin/awk -f
{
    if (substr($0, 0, 1) != "#") {
        timestamp = strftime("%D %T", systime());
        row = timestamp","$1","$2","$3;
        print(row);
        system("");
    }
}
