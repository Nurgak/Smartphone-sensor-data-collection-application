%ip = '192.168.0.107';
ip = '128.179.164.144';
port = 8888;
run = true;

xLimit = 100;
sampledValues = zeros(xLimit,2);

yLimit = 200;

% allow some reading retries when waiting for incoming data
retries = 10;

close all

t = tcpip(ip, port, 'NetworkRole', 'client');

% these delays are necessary for some reason
try
    fopen(t);
    pause(.1);
    fprintf(t, 'sensor start');
    pause(.1);
    % start the usb communication
    fprintf(t, 'usb connect');
    pause(.1);
    % start reading what comes from the usb port
    fprintf(t, 'usb -r');
    pause(.1);
catch exception
    disp('Could not connect, did you launch the application?');
    return
end

figure('name','Internal and external gyroscope real-time view');

subplot(2,2,1);

% add a stop button to the plot
buttonStop = uicontrol('Style', 'pushbutton', 'Callback', 'run = false;', 'String', 'Stop');

int = bar([0 0 0],'r');
title('Internal gyroscope (InvenSense MPU3050)');

% label axes and set limits
xlabel('Axis');
ylabel('Rotation (0/s)');
set(gca,'XTickLabel',{'x', 'y', 'z'})
set(gca,'YLim',[-yLimit yLimit]);

subplot(2,2,2);

ext = bar([0 0 0],'b');
title('External gyroscope (STMicroelectronics L3G4200D)');

% label axes and set limits
xlabel('Axis');
ylabel('Rotation (0/s)');
set(gca,'XTickLabel',{'x', 'y', 'z'})
set(gca,'YLim',[-yLimit yLimit]);

subplot(2,2,3:4);

% plot the values over time
tim = plot([1:length(sampledValues)], sampledValues,'YDataSource','sampledValues');
set(gcf, 'DefaultAxesColorOrder', [1 0 0;0 0 1]);
title('Time series: gyroscope values');

set(tim(1),'Displayname','Internal');
set(tim(2),'Displayname','External');
legend('Location','north');

xlabel('Time');
ylabel('Rotation (0/s)');
%set(gca,'YLim',[-yLimit yLimit]);

% start sampling
fprintf(t, 'sensor gyro');
pause(.1);

% 1 = internal, 2 = external
sensor = 1;
retry = retries;
timeout = clock;

rad2deg = 180 / pi;

timeSeriesStart = clock;

% as long as the connection is up loop here
while run
    if get(t, 'BytesAvailable') > 1
        % read data in, make the right format
        raw = fread(t, t.BytesAvailable, 'char')';
        data = str2num(char(raw(find(raw == '[', 1):length(raw)))) * rad2deg;
        
        % update plot
        if length(data) == 3
            try
                if sensor == 1
                    % do not allow more then xLimit values for the time series
                    if length(sampledValues) > xLimit - 1
                        sampledValues = sampledValues(2:end,:);
                    end
                    sampledValues = [sampledValues; data(3) 0];
                    set(int, 'YData', data);
                else
                    data = data / (36 / 0.0058) - 0.5;
                    sampledValues(end, 2) = data(2);
                    
                    refreshdata(tim, 'caller');
                    set(ext, 'YData', data);
                    refreshdata
                    drawnow
                end
                retry = 0;
            catch error
                continue
            end
        elseif retry > 0
            retry = retry - 1;
        end
        
        % ask for another data point, alternate sensors
        if retry == 0
            if sensor == 1
                sensor = 2;
                fprintf(t, 'usb -w g');
            else
                sensor = 1;
                fprintf(t, 'sensor gyro');
            end
            timeout = clock;
            retry = retries;
        end
    end
    
    % sometimes this hangs, quit gracefully
    if etime(clock, timeout) > 3
        disp('Connection lost.');
        break
    end
end

fclose(t);
delete(t);
clear t

close all
