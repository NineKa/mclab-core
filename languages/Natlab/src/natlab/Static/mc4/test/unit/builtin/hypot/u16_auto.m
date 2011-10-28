function [result] = u16_auto(n)
  x0 = single([0.844421851525,0.75795440294;0.420571580831,0.258916750293]*n);
  x1 = single([0.844421851525,0.75795440294;0.420571580831,0.258916750293]*n);
  result = hypot(x0,x1);
end
