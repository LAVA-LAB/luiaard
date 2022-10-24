CONF="-ci 95 --output_type pdf --save --verbose"
python plot_switching.py ../results/switching-weight-0.8/CHAIN_LARGE/ $CONF -o plots/switching_0_8_CHAIN_LARGE &
python plot_switching.py ../results/switching-weight-0.9/CHAIN_LARGE/ $CONF -o plots/switching_0_9_CHAIN_LARGE &
python plot_switching.py ../results/switching-weight-1.0/CHAIN_LARGE/ $CONF -o plots/switching_1_0_CHAIN_LARGE &
python plot_switching.py ../results/switching-weight-0.8/BETTING_GAME_FAVOURABLE/ $CONF -o plots/switching_0_8_BETTING_GAME &
python plot_switching.py ../results/switching-weight-0.9/BETTING_GAME_FAVOURABLE/ $CONF -o plots/switching_0_9_BETTING_GAME &
python plot_switching.py ../results/switching-weight-1.0/BETTING_GAME_FAVOURABLE/ $CONF -o plots/switching_1_0_BETTING_GAME &

wait

python plot_switching.py ../results/switching-weight-0.8/CHAIN_LARGE/ $CONF -o plots/switching_0_8_CHAIN_LARGE_unbounded --includes *_unbounded_*.csv &
python plot_switching.py ../results/switching-weight-0.8/CHAIN_LARGE/ $CONF -o plots/switching_0_8_CHAIN_LARGE_lowbound --includes *_lowbound_*.csv &
python plot_switching.py ../results/switching-weight-0.8/CHAIN_LARGE/ $CONF -o plots/switching_0_8_CHAIN_LARGE_highbound --includes *_highbound_*.csv &

python plot_switching.py ../results/switching-weight-0.8/BETTING_GAME_FAVOURABLE/ $CONF -o plots/switching_0_8_BETTING_GAME_unbounded --includes *_unbounded_*.csv &
python plot_switching.py ../results/switching-weight-0.8/BETTING_GAME_FAVOURABLE/ $CONF -o plots/switching_0_8_BETTING_GAME_lowbound --includes *_lowbound_*.csv &
python plot_switching.py ../results/switching-weight-0.8/BETTING_GAME_FAVOURABLE/ $CONF -o plots/switching_0_8_BETTING_GAME_highbound --includes *_highbound_*.csv &

wait
echo "finished"
