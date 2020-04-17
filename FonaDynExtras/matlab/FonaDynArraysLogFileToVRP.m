function [names, dataArray, vrpArray] = FonaDynArraysLogFileToVRP(data, nClusters)
% Log file columns from FonaDyn 2.0 are loaded; this order is expected:
% [time, freq, amp, clarity, crest, cluster_number, sampen, iContact, dEGGmax, qContact] ++ amps ++ phases
% dataArray becomes a sparse array (1:layers,1:fo_values,1:spl_values] corresponding to the voice field.
% dataArray and names can be passed to FonaDynPlotVRP or FonaDynPlotVRPratios/-diffs
% vrpArray is filled as for a _VRP.csv format file with one cell per row.
% names is filled with the column names for that file.

names = {'MIDI', 'SPL', 'Total', 'Clarity', 'Crest', 'Entropy', 'Icontact', 'dEGGmax', 'Qcontact', 'maxCluster'};
for c = 1 : nClusters
    names{10+c} = char( ['Cluster ' num2str(i)]);
end

VRPx=zeros(8+nClusters,66,80);
%VRPx(VRPx==0)=NaN;

for i = 1 : size(data, 1)
    foIx = max(1, round(data(i, 2)) - 29);
    splIx  = max(1, round(data(i, 3)) - 39);
    VRPx(1, foIx, splIx) = VRPx(1, foIx, splIx)  +  1 ;   % accumulate total cycles
    VRPx(2, foIx, splIx) = data(i, 4);   % latest clarity
    VRPx(3, foIx, splIx) = VRPx(3, foIx, splIx)  +  data(i, 5);   % accumulate crest factor
    VRPx(4, foIx, splIx) = max (VRPx(4, foIx, splIx), data(i, 7)); % keep the max entropy
    VRPx(5, foIx, splIx) = max (VRPx(5, foIx, splIx), data(i, 8)); % accumulate iContact
    VRPx(6, foIx, splIx) = VRPx(6, foIx, splIx) + data(i, 9); % accumulate dEGGmax/Qdelta
    VRPx(7, foIx, splIx) = VRPx(7, foIx, splIx) + data(i, 10); % accumulate qContact
    clusterNo = data(i, 6)+1;   % get the cluster number of this cycle
    VRPx(8+clusterNo, foIx, splIx) = VRPx(8+clusterNo, foIx, splIx) + 1; % accumulate cycles per cluster
end

vArr = [];
row = 1;
for foIx = 1 : 66
    for splIx = 1 : 80
        totCycles = VRPx(1, foIx, splIx);
        if totCycles > 0
            VRPx(3, foIx, splIx) = VRPx(3, foIx, splIx) / totCycles;
            VRPx(5, foIx, splIx) = VRPx(5, foIx, splIx) / totCycles;
            VRPx(6, foIx, splIx) = VRPx(6, foIx, splIx) / totCycles;
            VRPx(7, foIx, splIx) = VRPx(7, foIx, splIx) / totCycles;
            [maxVal maxIx] = max(VRPx(9:13, foIx, splIx));
            VRPx(8, foIx, splIx) = maxIx;
            vArr(row, :) = [foIx+29; splIx+39; VRPx(1:13, foIx, splIx)]';
            row = row+1;
        end
    end
end
dataArray = VRPx;
vrpArray = vArr;
end