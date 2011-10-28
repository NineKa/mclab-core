function [result] = u40_auto(n)
  x0 = char(32+80*[0.236048089737,0.103166034231;0.396058242611,0.154972270802]*n);
  x1 = single([0.844421851525,0.75795440294;0.420571580831,0.258916750293]*n);
  result = rem(x0,x1);
end
