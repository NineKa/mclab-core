function [result] = u45_auto(n)
  x0 = char(32+80*[0.236048089737,0.103166034231;0.396058242611,0.154972270802]*n);
  x1 = ([0.62290169489,0.741786989261;0.795193565566,0.942450283777]*n > .5);
  result = complex(x0,x1);
end
